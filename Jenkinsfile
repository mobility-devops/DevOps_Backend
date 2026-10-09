// DevOps_Backend CI (아키텍처 5장)
// - PR(feature → develop): migration 검사 → 테스트·JaCoCo → SonarQube Cloud Quality Gate
// - develop: 테스트 → Sonar → 이미지 빌드(dev-<shortSHA>) → Trivy → GHCR push → digest
// 비밀값은 코드에 넣지 않고 Jenkins Credentials ID 로만 참조한다.
pipeline {
    agent any

    options {
        disableConcurrentBuilds()
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    environment {
        IMAGE = 'ghcr.io/mobility-devops/taxi-backend'
        SOURCE_URL = 'https://github.com/mobility-devops/DevOps_Backend'
    }

    stages {
        // Canary 중에는 옛 버전과 새 버전이 같은 DB 를 쓰므로 확장 변경만 허용한다 (아키텍처 7장).
        // 축소 변경은 파일 이름에 __contract_ 를 넣어 검사에서 빼고 리뷰에서 확인한다.
        stage('Migration 검사') {
            when { changeRequest() }
            steps {
                sh '''
                    set -eu
                    git fetch -q --no-tags origin "+refs/heads/${CHANGE_TARGET}:refs/remotes/origin/${CHANGE_TARGET}"
                    NEW=$(git diff --name-only --diff-filter=A "origin/${CHANGE_TARGET}...HEAD" -- src/main/resources/db/migration | grep -v '__contract_' || true)
                    echo "새 migration: ${NEW:-없음}"
                    [ -z "$NEW" ] && exit 0
                    if grep -niE '\\b(DROP|RENAME|MODIFY)\\b' $NEW; then
                        echo '축소 변경(DROP, RENAME, MODIFY)은 금지. 꼭 필요하면 파일 이름에 __contract_ 를 넣고 리뷰에서 확인할 것'
                        exit 1
                    fi
                '''
            }
        }

        // 매번 새로 뜨는 JDK 컨테이너에서 빌드한다.
        // Testcontainers 는 ci-01 Docker 를 쓰고 MySQL 컨테이너에는 localhost 로 접속한다 (ufw 변경 불필요).
        stage('테스트·품질') {
            agent {
                docker {
                    image 'eclipse-temurin:21-jdk'
                    args '--network host --group-add 986 -v /var/run/docker.sock:/var/run/docker.sock -v /var/lib/jenkins/gradle-cache:/gradle-cache'
                    reuseNode true
                }
            }
            environment {
                GRADLE_USER_HOME = '/gradle-cache'
                TESTCONTAINERS_HOST_OVERRIDE = 'localhost'
                SONAR_USER_HOME = '/gradle-cache/sonar'
            }
            stages {
                stage('테스트') {
                    steps {
                        sh './gradlew --no-daemon test jacocoTestReport'
                    }
                    post {
                        always {
                            junit allowEmptyResults: true, testResults: 'build/test-results/test/*.xml'
                        }
                    }
                }
                stage('SonarQube') {
                    when { anyOf { changeRequest(); branch 'develop' } }
                    steps {
                        withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
                            sh './gradlew --no-daemon sonar -Dsonar.qualitygate.wait=true'
                        }
                    }
                }
            }
        }

        stage('이미지 빌드') {
            when { branch 'develop' }
            steps {
                script {
                    env.IMAGE_TAG = 'dev-' + sh(script: 'git rev-parse --short=7 HEAD', returnStdout: true).trim()
                }
                sh '''
                    set -eu
                    docker build --pull \
                        --label org.opencontainers.image.source="$SOURCE_URL" \
                        --label org.opencontainers.image.revision="$(git rev-parse HEAD)" \
                        -t "$IMAGE:$IMAGE_TAG" .
                    test "$(docker inspect --format '{{.Config.User}}' "$IMAGE:$IMAGE_TAG")" = 10001
                '''
            }
        }

        // 고칠 수 있는 CRITICAL 이 있으면 실패, HIGH 는 보고만, 비밀값이 있으면 실패
        stage('Trivy') {
            when { branch 'develop' }
            steps {
                sh '''
                    set -eu
                    trivy image --no-progress --scanners vuln --severity CRITICAL --ignore-unfixed --exit-code 1 "$IMAGE:$IMAGE_TAG"
                    trivy image --no-progress --scanners vuln --severity HIGH --ignore-unfixed --exit-code 0 "$IMAGE:$IMAGE_TAG"
                    trivy image --no-progress --scanners secret --exit-code 1 "$IMAGE:$IMAGE_TAG"
                '''
            }
        }

        // 로그인 정보는 이번 빌드 전용 임시 폴더에만 두고 바로 지운다.
        stage('GHCR push') {
            when { branch 'develop' }
            steps {
                withCredentials([usernamePassword(credentialsId: 'ghcr-push', usernameVariable: 'GHCR_USER', passwordVariable: 'GHCR_TOKEN')]) {
                    sh '''
                        set -eu
                        export DOCKER_CONFIG="$(mktemp -d)"
                        trap 'rm -rf "$DOCKER_CONFIG"' EXIT
                        printf '%s' "$GHCR_TOKEN" | docker login ghcr.io -u "$GHCR_USER" --password-stdin
                        docker push "$IMAGE:$IMAGE_TAG"
                        docker inspect --format '{{index .RepoDigests 0}}' "$IMAGE:$IMAGE_TAG" > image-digest.txt
                    '''
                }
                script {
                    def ref = readFile('image-digest.txt').trim()
                    currentBuild.description = "${env.IMAGE_TAG} ${ref.substring(ref.indexOf('@') + 1)}"
                    echo "배포용 digest: ${ref}"
                }
                archiveArtifacts artifacts: 'image-digest.txt'
            }
        }
    }

    post {
        always {
            script {
                if (env.IMAGE_TAG) {
                    sh 'docker rmi "$IMAGE:$IMAGE_TAG" || true'
                }
            }
        }
    }
}
