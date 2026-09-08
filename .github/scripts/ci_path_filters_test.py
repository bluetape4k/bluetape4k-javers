"""CI의 경로 선택 계약을 실제 workflow 설정으로 검증한다."""
import fnmatch
from pathlib import Path
import re
import unittest

WORKFLOW = Path(__file__).resolve().parents[1] / "workflows" / "ci.yml"


def filters():
    block = WORKFLOW.read_text().split("          filters: |\n", 1)[1].split("\n  #", 1)[0]
    result = {}
    current = None
    for line in block.splitlines():
        key = re.fullmatch(r"            ([a-z0-9-]+):", line)
        pattern = re.fullmatch(r"              - '([^']+)'", line)
        if key:
            current = key[1]
            result[current] = []
        elif pattern and current:
            result[current].append(pattern[1])
    return result


def selected(path):
    # pull_request/push의 기존 paths-ignore 계약과 동일한 문서 경계다.
    if path.endswith(".md") or path.startswith("docs/"):
        return set()
    return {key for key, patterns in filters().items()
            if any(fnmatch.fnmatchcase(path, pattern) for pattern in patterns)}


class PathFilterContractTest(unittest.TestCase):
    def test_common_inputs_select_all_nine_modules(self):
        self.assertEqual(len(filters()), 9)
        for path in ["build.gradle.kts", "settings.gradle.kts", "gradle.properties",
                     "gradle/libs.versions.toml", "gradle/wrapper/gradle-wrapper.properties",
                     "gradlew", "gradlew.bat", "buildSrc/src/main/kotlin/Build.kt"]:
            with self.subTest(path=path):
                self.assertEqual(selected(path), set(filters()))

    def test_core_selects_all_consumers(self):
        self.assertEqual(selected("javers-core/src/main/kotlin/Codec.kt"), set(filters()))

    def test_exposed_selects_test_and_runtime_consumers(self):
        self.assertEqual(selected("javers-exposed/src/main/kotlin/Repository.kt"), {
            "javers-exposed", "javers-ddd", "javers-kafka", "javers-spring-boot4-autoconfigure",
            "examples-javers-exposed-ddd", "examples-javers-ktor", "examples-javers-spring-boot4"})

    def test_redis_selects_kafka_test_and_autoconfigure_consumers(self):
        self.assertEqual(selected("javers-persistence-redis/src/main/kotlin/Repository.kt"),
                         {"javers-redis", "javers-kafka", "javers-spring-boot4-autoconfigure"})

    def test_ddd_selects_only_its_runtime_consumers(self):
        self.assertEqual(selected("javers-ddd/src/main/kotlin/Event.kt"),
                         {"javers-ddd", "examples-javers-exposed-ddd", "examples-javers-ktor",
                          "examples-javers-spring-boot4"})

    def test_shared_contract_selects_three_examples(self):
        self.assertEqual(selected("examples/shared-test-contracts/OrderBehaviorContract.kt"),
                         {"examples-javers-exposed-ddd", "examples-javers-ktor",
                          "examples-javers-spring-boot4"})

    def test_example_is_isolated(self):
        self.assertEqual(selected("examples/javers-ktor/src/main/kotlin/App.kt"), {"examples-javers-ktor"})

    def test_docs_and_benchmark_do_not_expand_functional_matrix(self):
        for path in ["README.md", "docs/guide.md", "javers-core/README.ko.md",
                     "benchmark/javers-exposed-benchmark/src/main/kotlin/Benchmark.kt"]:
            with self.subTest(path=path):
                self.assertEqual(selected(path), set())


if __name__ == "__main__":
    unittest.main()
