Small project to learn about CFG
================================

This is a small project to learn about [CFG (Control Flow Graph) based on ObjectWeb ASM](https://asm.ow2.io/developer-guide.html#controlflow).

To visualize result, use [`dot` command](https://www.graphviz.org/doc/info/command.html) to generate PNG file. For instance, the following example will generate a PNG file from `result.dot` in current working directory:

```bash
$ dot result.dot -T png -o out.png
```

To format Java codes, run `./gradlew spotlessApply` that is supported by [spotless plugin](https://github.com/diffplug/spotless).

To upgrade dependencies and plugins, run `./gradlew useLatestVersions` that is supported by [use-latest-versions plugin](https://github.com/patrikerdes/gradle-use-latest-versions-plugin).

Copyright
---------

Copyright 2019 Kengo TODA

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
