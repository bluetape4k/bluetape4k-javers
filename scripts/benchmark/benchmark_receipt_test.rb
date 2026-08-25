require "json"
require "minitest/autorun"
require "tmpdir"

require_relative "benchmark_receipt"
require_relative "enrich_benchmark_receipts"

class BenchmarkReceiptTest < Minitest::Test
  def test_accepts_complete_jmh_receipts_for_both_smoke_scenarios
    with_receipts do |root|
      result = BenchmarkReceipts::Validator.new(root).validate

      assert_empty result.errors
      assert_equal 24, result.record_count
    end
  end

  def test_rejects_missing_variant_and_scenario
    with_receipts(remove: ["both", "dateRangeQuery"]) do |root|
      result = BenchmarkReceipts::Validator.new(root).validate

      assert result.errors.any? { |error| error.include?("missing commit-metadata receipt") }
      assert result.errors.any? { |error| error.include?("missing Envers receipt") }
    end
  end

  def test_rejects_duplicate_matrix_rows_from_a_stale_attempt
    with_receipts do |root|
      path = File.join(root, "commit.json")
      records = JSON.parse(File.read(path))
      records << records.first
      File.write(path, JSON.pretty_generate(records))

      result = BenchmarkReceipts::Validator.new(root).validate

      assert result.errors.any? { |error| error.include?("duplicate commit-metadata receipt") }
    end
  end

  def test_rejects_empty_receipt_file
    with_receipts do |root|
      File.write(File.join(root, "empty.json"), "[]\n")

      result = BenchmarkReceipts::Validator.new(root).validate

      assert_includes result.errors, "empty benchmark receipt: empty.json"
    end
  end

  def test_rejects_incomplete_primary_metric
    with_receipts do |root|
      path = File.join(root, "commit.json")
      records = JSON.parse(File.read(path))
      records.first["primaryMetric"].delete("rawData")
      File.write(path, JSON.pretty_generate(records))

      result = BenchmarkReceipts::Validator.new(root).validate

      assert result.errors.any? { |error| error.include?("primaryMetric.rawData") }
    end
  end

  def test_rejects_teardown_failure_receipt
    with_receipts do |root|
      File.write(
        File.join(root, "teardown-failures.jsonl"),
        JSON.generate("owner" => "benchmark", "resource" => "schema", "message" => "drop failed") + "\n",
      )

      result = BenchmarkReceipts::Validator.new(root).validate

      assert result.errors.any? { |error| error.include?("teardown failure receipt") }
    end
  end

  def test_fails_closed_when_no_json_receipts_exist
    Dir.mktmpdir("benchmark-receipt") do |root|
      result = BenchmarkReceipts::Validator.new(root).validate

      assert_equal ["no benchmark JSON receipts found"], result.errors
    end
  end

  def test_enricher_adds_provenance_to_generated_jmh_rows
    Dir.mktmpdir("benchmark-receipt") do |root|
      path = File.join(root, "receipt.json")
      File.write(path, JSON.pretty_generate(["benchmark" => "fixture", "params" => {}]))

      count = BenchmarkReceipts::Enricher.enrich(
        root,
        source_command: "./gradlew smoke",
        generated_at: "2026-08-25T00:00:00Z",
      )
      row = JSON.parse(File.read(path)).first

      assert_equal 1, count
      assert_equal "2026-08-25T00:00:00Z", row["generatedAt"]
      assert_equal "./gradlew smoke", row["sourceCommand"]
    end
  end

  private

  def with_receipts(remove: [])
    Dir.mktmpdir("benchmark-receipt") do |root|
      commit_records = []
      envers_records = []
      %w[baseline author commit_date both].each_with_index do |variant, index|
        %w[insert authorQuery dateRangeQuery].each do |scenario|
          commit_records << jmh_record(
            benchmark: "io.bluetape4k.javers.benchmark.exposed.ExposedCommitMetadataIndexBenchmark.#{scenario}",
            params: { "variantName" => variant },
          ) unless remove.include?(variant) || remove.include?(scenario)
        end
        %w[insert update auditQuery].each do |scenario|
          envers_records << jmh_record(
            benchmark: "io.bluetape4k.javers.benchmark.exposed.EnversComparisonBenchmark.#{scenario}",
            params: { "implementationName" => %w[envers javers_in_memory javers_exposed_repository javers_exposed_ddd][index] },
          ) unless remove.include?(variant) || remove.include?(scenario)
        end
      end
      File.write(File.join(root, "commit.json"), JSON.pretty_generate(commit_records))
      File.write(File.join(root, "envers.json"), JSON.pretty_generate(envers_records))
      yield root
    end
  end

  def jmh_record(benchmark:, params:)
    {
      "jmhVersion" => "1.37",
      "benchmark" => benchmark,
      "mode" => "thrpt",
      "threads" => 1,
      "forks" => 1,
      "params" => params,
      "primaryMetric" => {
        "score" => 1.0,
        "scoreUnit" => "ops/s",
        "rawData" => [[1.0]],
      },
      "generatedAt" => "2026-08-25T00:00:00Z",
      "sourceCommand" => "./gradlew smoke",
    }
  end
end
