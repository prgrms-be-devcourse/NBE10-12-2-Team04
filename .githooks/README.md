# Git Hooks 규칙

이 저장소는 로컬 Git Hooks 경로를 `.githooks`로 지정해서 팀 규칙을 검사합니다.

처음 클론한 팀원은 아래 명령어를 한 번 실행해야 훅이 적용됩니다.

```bash
git config core.hooksPath .githooks
```

## 적용 중인 검사

- `pre-commit`: 커밋 전 브랜치명 규칙 검사
- `pre-push`: 푸시 전 브랜치명 규칙 검사
- `commit-msg`: 커밋 메시지 규칙 검사

## 브랜치 규칙

허용되는 브랜치명:

```text
main
develop
feature/issue-1-signup-login
fix/issue-6-login-validation
docs/issue-5-readme-rule
build/issue-26-gradle-dependencies
chore/issue-26-branch-convention
```

## 커밋 규칙

```text
type: 세부 작업내용 #이슈번호
```

허용되는 타입:

```text
feat, fix, docs, style, refactor, test, chore, build, ci, perf, revert
```

예시:

```text
feat: 좋아요 엔티티 추가 #12
fix: 로그인 유효성 검사 오류 수정 #6
docs: 리드미 작성 #5
build: Gradle 의존성 선언 방식 정리 #26
chore: 브랜치 네이밍 컨벤션 정리 #26
```
