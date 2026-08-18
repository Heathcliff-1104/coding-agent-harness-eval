# DeepSeek Harness run

- Upstream: https://github.com/deepseek-ai/deepseek-harness
- Commit: `47f943859bef60e4160492346772ded9b24f765a`
- Repository version at run time: `0.1.0-rc.5`
- Profile: official `headless`
- Model: profile default `deepseek-v4-flash`
- Network: allowed

The evaluation retained the harness's native workflow and subagent design:

- `maxConcurrentAgents: 10`
- `maxTotalAgents: 40`
- Ralph subagent provider: `spawn`
- `maxRounds: 24`

The exact overlay is in `config/native-eval.patch.yml`; the fully composed profile is in `composed-config.txt`; the task is in `task.txt`.

