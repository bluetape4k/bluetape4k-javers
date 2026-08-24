require "json"
require "time"

module BenchmarkReceipts
  module Enricher
    module_function

    def enrich(root, source_command:, generated_at: Time.now.utc.iso8601)
      paths = Dir.glob(File.join(File.expand_path(root), "**", "*.json")).sort
      raise ArgumentError, "no benchmark JSON receipts found" if paths.empty?

      paths.each do |path|
        payload = JSON.parse(File.read(path))
        raise ArgumentError, "benchmark receipt must be a JSON array: #{path}" unless payload.is_a?(Array)

        payload.each do |record|
          record["generatedAt"] ||= generated_at
          record["sourceCommand"] ||= source_command
        end
        File.write(path, JSON.pretty_generate(payload) + "\n")
      end
      paths.length
    end
  end
end

if $PROGRAM_NAME == __FILE__
  begin
    root = ARGV.fetch(0, "benchmark/javers-exposed-benchmark/build/reports/benchmarks")
    source_command = ARGV.fetch(1, "./gradlew benchmark smoke")
    count = BenchmarkReceipts::Enricher.enrich(root, source_command: source_command)
    puts "benchmark receipts enriched: #{count} files"
  rescue JSON::ParserError, ArgumentError => e
    warn "ERROR: #{e.message}"
    exit 1
  end
end
