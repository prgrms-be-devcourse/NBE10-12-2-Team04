#!/bin/sh

# 현재 체크아웃된 브랜치명을 가져옵니다.
branch_name=$(git symbolic-ref --quiet --short HEAD 2>/dev/null)

# detached HEAD 상태에서는 브랜치명이 없으므로 검사를 건너뜁니다.
if [ -z "$branch_name" ]; then
  exit 0
fi

# 기본 브랜치는 그대로 허용합니다.
if printf '%s\n' "$branch_name" | grep -Eq '^(main|develop)$'; then
  exit 0
fi

# 작업 브랜치는 type + issue 번호 + 작업내용 형식만 허용합니다.
if printf '%s\n' "$branch_name" | grep -Eq '^(feature|refactor|fix|docs|build|chore)/issue-[0-9]+-[A-Za-z0-9._-]+$'; then
  exit 0
fi

cat <<EOF
브랜치명 규칙에 맞지 않습니다: $branch_name

허용되는 브랜치명:
  main
  develop
  feature/issue-1-signup-login
  refactor/issue-1-signup-login
  fix/issue-6-login-validation
  docs/issue-5-readme-rule
  build/issue-26-gradle-dependencies
  chore/issue-26-branch-convention
EOF

exit 1
