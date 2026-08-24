require "json"
require "time"

module BenchmarkReceipts
  Result = Struct.new(:errors, :record_count, keyword_init: true)

  class Validator
    COMMIT_BENCHMARK = "io.bluetape4k.javers.benchmark.exposed.ExposedCommitMetadataIndexBenchmark".freeze
    ENVERS_BENCHMARK = "io.bluetape4k.javers.benchmark.exposed.EnversComparisonBenchmark".freeze
    COMMIT_VARIANTS = %w[baseline author commit_date both].freeze
    COMMIT_SCENARIOS = %w[insert authorQuery dateRangeQuery].freeze
    ENVERS_VARIANTS = %w[envers javers_in_memory javers_exposed_repository javers_exposed_ddd].freeze
    ENVERS_SCENARIOS = %w[insert update auditQuery].freeze

    def initialize(root)
      @root = File.expand_path(root)
    end

    def validate
      errors = []
      paths = Dir.glob(File.join(@root, "**", "*.json")).sort
      if paths.empty?
        return Result.new(errors: ["no benchmark JSON receipts found"], record_count: 0)
      end

      records = []
      paths.each do |path|
        payload = JSON.parse(File.read(path))
        unless payload.is_a?(Array)
          errors << "benchmark receipt must be a JSON array: #{relative(path)}"
          next
        end
        if payload.empty?
          errors << "empty benchmark receipt: #{relative(path)}"
          next
        end
        payload.each_with_index do |record, index|
          validate_record(record, relative(path), index, errors)
          records << record if record.is_a?(Hash)
        end
      rescue JSON::ParserError => e
        errors << "invalid benchmark JSON #{relative(path)}: #{e.message}"
      rescue SystemCallError => e
        errors << "unreadable benchmark JSON #{relative(path)}: #{e.message}"
      end

      validate_expected_records(records, errors)
      validate_teardown_failures(errors)
      Result.new(errors: errors, record_count: records.length)
    end

    private

    def validate_record(record, path, index, errors)
      prefix = "#{path}[#{index}]"
      unless record.is_a?(Hash)
        errors << "benchmark receipt record must be an object: #{prefix}"
        return
      end

      required(record, "benchmark", String, prefix, errors)
      required(record, "mode", String, prefix, errors)
      required(record, "params", Hash, prefix, errors)
      required(record, "generatedAt", String, prefix, errors)
      required(record, "sourceCommand", String, prefix, errors)
      %w[threads forks].each do |key|
        value = record[key]
        errors << "#{prefix}.#{key} must be a positive integer" unless value.is_a?(Integer) && value.positive?
      end
      begin
        Time.iso8601(record["generatedAt"].to_s)
      rescue ArgumentError
        errors << "#{prefix}.generatedAt must be ISO-8601"
      end

      metric = record["primaryMetric"]
      unless metric.is_a?(Hash)
        errors << "#{prefix}.primaryMetric must be an object"
        return
      end
      score = metric["score"]
      errors << "#{prefix}.primaryMetric.score must be finite and positive" unless score.is_a?(Numeric) && score.finite? && score.positive?
      errors << "#{prefix}.primaryMetric.scoreUnit must be non-empty" unless metric["scoreUnit"].is_a?(String) && !metric["scoreUnit"].empty?
      raw_data = metric["rawData"]
      errors << "#{prefix}.primaryMetric.rawData must be non-empty" unless raw_data.is_a?(Array) && !raw_data.empty?
    end

    def required(record, key, type, prefix, errors)
      value = record[key]
      valid = value.is_a?(type) && (!value.respond_to?(:empty?) || !value.empty?)
      errors << "#{prefix}.#{key} is required" unless valid
    end

    def validate_expected_records(records, errors)
      commit_keys = records.each_with_object([]) do |record, keys|
        next unless record["benchmark"].to_s.start_with?(COMMIT_BENCHMARK + ".")
        keys << [record.dig("params", "variantName"), record["benchmark"].to_s.delete_prefix(COMMIT_BENCHMARK + ".")]
      end
      expected_commit_keys = COMMIT_VARIANTS.product(COMMIT_SCENARIOS)
      validate_matrix(commit_keys, expected_commit_keys, "commit-metadata", errors)

      envers_keys = records.each_with_object([]) do |record, keys|
        next unless record["benchmark"].to_s.start_with?(ENVERS_BENCHMARK + ".")
        keys << [record.dig("params", "implementationName"), record["benchmark"].to_s.delete_prefix(ENVERS_BENCHMARK + ".")]
      end
      expected_envers_keys = ENVERS_VARIANTS.product(ENVERS_SCENARIOS)
      validate_matrix(envers_keys, expected_envers_keys, "Envers", errors)
    end

    def validate_matrix(actual, expected, label, errors)
      counts = actual.each_with_object(Hash.new(0)) { |key, tally| tally[key] += 1 }
      counts.each do |key, count|
        next unless count > 1

        errors << "duplicate #{label} receipt: #{key.join("/")} (count=#{count})"
      end

      (actual.uniq - expected).each do |first, second|
        errors << "unexpected #{label} receipt: #{first}/#{second}"
      end

      (expected - actual.uniq).each do |first, second|
        errors << "missing #{label} receipt: #{first}/#{second}"
      end
    end

    def validate_teardown_failures(errors)
      paths = Dir.glob(File.join(@root, "**", "*teardown-failures.jsonl")).sort
      paths.each do |path|
        File.foreach(path).with_index do |line, index|
          next if line.strip.empty?
          payload = JSON.parse(line)
          unless payload.is_a?(Hash) && %w[owner resource message].all? { |key| payload[key].is_a?(String) && !payload[key].empty? }
            errors << "invalid teardown failure receipt: #{relative(path)}:#{index + 1}"
            next
          end
          errors << "teardown failure receipt: #{relative(path)}:#{index + 1} owner=#{payload['owner']} resource=#{payload['resource']} message=#{payload['message']}"
        rescue JSON::ParserError => e
          errors << "invalid teardown failure receipt #{relative(path)}:#{index + 1}: #{e.message}"
        end
      end
    end

    def relative(path)
      path.delete_prefix(@root + "/")
    end
  end
end

if $PROGRAM_NAME == __FILE__
  root = ARGV.fetch(0, "benchmark/javers-exposed-benchmark/build/reports/benchmarks")
  result = BenchmarkReceipts::Validator.new(root).validate
  if result.errors.empty?
    puts "benchmark receipts valid: #{result.record_count} records"
    exit 0
  end

  warn result.errors.map { |error| "ERROR: #{error}" }
  exit 1
end
