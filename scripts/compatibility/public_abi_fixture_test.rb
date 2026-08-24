#!/usr/bin/env ruby
# frozen_string_literal: true

require "pathname"

root = Pathname.new(__dir__).join("../..").realpath

fixtures = {
  root.join("javers-core/api/javers-core.api") => {
    "EntityEnvelope companion" => ['EntityEnvelope$Companion'],
    "Kotlin default constructor" => ['synthetic fun <init>', 'DefaultConstructorMarker'],
  },
  root.join("javers-persistence-kafka/api/javers-persistence-kafka.api") => {
    "JVM constructor overload" => ['KafkaCdoSnapshotRepository :', 'KafkaTemplate'],
    "Kotlin companion default overload" => ['KafkaSnapshotEventPublisher$Companion', 'invoke$default'],
  },
}

failures = []
fixtures.each do |path, expectations|
  unless path.file?
    failures << "ABI baseline missing: #{path}"
    next
  end

  content = path.read
  expectations.each do |name, fragments|
    failures << "#{name} missing from #{path}" unless fragments.all? { |fragment| content.include?(fragment) }
  end
end

abort failures.join("\n") unless failures.empty?

puts "Public ABI fixture contract: PASS (Kotlin default arguments, companion, and JVM constructors are represented)."
