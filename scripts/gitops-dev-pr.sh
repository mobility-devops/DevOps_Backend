#!/usr/bin/env bash
# develop 이미지가 GHCR 에 올라간 뒤, GitOps dev overlay 의 이미지 digest 한 줄만 바꾸는 PR 을 만들고 자동 머지를 켠다.
# (dev 폴더는 CODEOWNERS 담당자가 없어 승인 없이 머지된다. prod 는 대상이 아니다.)
#
# 환경변수
#   GH_TOKEN     GitHub App 설치 토큰 (DevOps_GitOps 의 Contents·Pull requests 쓰기 권한)
#   IMAGE_TAG    dev-<짧은SHA 7자리>
#   DIGEST_FILE  `ghcr.io/...@sha256:...` 한 줄이 든 파일 (기본 image-digest.txt)
#   DRY_RUN=1    clone·수정·diff 확인까지만 하고 push·PR 은 하지 않는다
#   GITOPS_URL   (시험용) GitOps 저장소 주소를 바꾼다. 기본은 GitHub 저장소
set -euo pipefail
set +x   # 토큰이 로그에 찍히지 않게

REPO="mobility-devops/DevOps_GitOps"
FILE="apps/taxi/overlays/dev/kustomization.yaml"
GITOPS_URL="${GITOPS_URL:-https://github.com/$REPO.git}"
DIGEST="$(sed 's/.*@//' "${DIGEST_FILE:-image-digest.txt}")"

[[ "$DIGEST" =~ ^sha256:[0-9a-f]{64}$ ]] || { echo "digest 형식 오류: $DIGEST"; exit 1; }
[[ "${IMAGE_TAG:-}" =~ ^dev-[0-9a-f]{7}$ ]] || { echo "IMAGE_TAG 형식 오류: ${IMAGE_TAG:-}"; exit 1; }

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
AUTH="$(printf 'x-access-token:%s' "${GH_TOKEN:-}" | base64 -w0)"
git_gh() { git -c "http.https://github.com/.extraheader=AUTHORIZATION: basic $AUTH" "$@"; }

git_gh clone -q --depth 1 "$GITOPS_URL" "$WORK/gitops"
cd "$WORK/gitops"

if grep -q "digest: $DIGEST" "$FILE"; then
  echo "이미 같은 digest 입니다. 할 일 없음."
  exit 0
fi

BRANCH="deploy/${IMAGE_TAG}"
if git_gh ls-remote --exit-code --heads origin "$BRANCH" >/dev/null 2>&1; then
  echo "원격에 $BRANCH 브랜치가 이미 있습니다. 이전 실행의 PR 을 확인하세요."
  exit 1
fi

git checkout -q -b "$BRANCH"
sed -i -E 's#^([[:space:]]*digest: )sha256:[0-9a-f]{64}$#\1'"$DIGEST"'#' "$FILE"

# digest 한 줄(추가 1 + 삭제 1) 외에 바뀌면 중단
CHANGED="$(git diff --numstat | awk '{s+=$1+$2} END{print s+0}')"
git diff --stat
[[ "$CHANGED" == "2" ]] || { echo "digest 한 줄 외 변경이 감지되어 중단합니다."; exit 1; }

if [[ "${DRY_RUN:-0}" == "1" ]]; then
  echo "DRY_RUN: push·PR 생략"
  exit 0
fi

git -c user.name="jenkins-ci" -c user.email="jenkins-ci@users.noreply.github.com" \
    commit -q -am "deploy: dev 이미지 digest 지정 ($IMAGE_TAG)"
git_gh push -q origin "$BRANCH"

API="https://api.github.com"
H=(-H "Authorization: Bearer $GH_TOKEN" -H "Accept: application/vnd.github+json")
BODY="Jenkins(CI)가 만든 PR 입니다. dev overlay 의 이미지 digest 한 줄만 바꿉니다.\\n\\n- 이미지: ghcr.io/mobility-devops/taxi-backend:${IMAGE_TAG}\\n- digest: ${DIGEST}\\n- 빌드: ${BUILD_URL:-}"
PR="$(curl -fsS -X POST "${H[@]}" "$API/repos/$REPO/pulls" \
  -d "{\"title\":\"deploy: dev 이미지 digest 지정 ($IMAGE_TAG)\",\"head\":\"$BRANCH\",\"base\":\"main\",\"body\":\"$BODY\"}")"
NUMBER="$(printf '%s' "$PR" | sed -n 's/.*"number": *\([0-9]*\).*/\1/p' | head -1)"
NODE_ID="$(printf '%s' "$PR" | sed -n 's/.*"node_id": *"\(PR_[^"]*\)".*/\1/p' | head -1)"
[[ -n "$NUMBER" && -n "$NODE_ID" ]] || { echo "PR 생성 응답을 해석하지 못했습니다."; exit 1; }
echo "PR #$NUMBER 생성"

# 자동 머지를 켠다. 이미 바로 머지할 수 있는 상태라서 실패하면 곧바로 squash 머지한다.
if ! curl -fsS -X POST "${H[@]}" "$API/graphql" \
  -d "{\"query\":\"mutation{enablePullRequestAutoMerge(input:{pullRequestId:\\\"$NODE_ID\\\",mergeMethod:SQUASH}){pullRequest{number}}}\"}" \
  | grep -q '"pullRequest"'; then
  echo "자동 머지 설정이 안 되어 바로 squash 머지합니다."
  curl -fsS -X PUT "${H[@]}" "$API/repos/$REPO/pulls/$NUMBER/merge" -d '{"merge_method":"squash"}' >/dev/null
fi
echo "완료: PR #$NUMBER (dev digest → $DIGEST)"
