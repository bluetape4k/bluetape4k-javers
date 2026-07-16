#!/usr/bin/env ruby
# frozen_string_literal: true

require "rexml/document"

ROOT = File.expand_path("../..", __dir__)
ASSETS = {
  "overview/repository-learning-map" => { cards: 8, connectors: 9, markers: 6 },
  "architecture/audit-snapshot-model" => { cards: 7, connectors: 8, markers: 6 },
  "persistence/persistence-decision-map" => { cards: 8, connectors: 9, markers: 6 },
  "persistence/exposed-snapshot-flow" => { cards: 7, connectors: 8, markers: 6 },
  "examples/ddd-cqrs-sequence" => { participants: 7, headers: 7, lifelines: 7, messages: 8, numbered_labels: 8, activations: 4, connectors: 8, frames: 1, markers: 6 }
}.freeze

failures = []

ASSETS.each do |rel, expected|
  svg = File.join(ROOT, "docs/manual/assets", "#{rel}.svg")
  png = File.join(ROOT, "docs/manual/assets", "#{rel}.png")
  unless File.file?(svg) && File.file?(png)
    failures << "#{rel}: missing SVG or PNG"
    next
  end

  doc = REXML::Document.new(File.read(svg))
  root = doc.root
  failures << "#{rel}: canvas must be 1600x1040" unless root.attributes["width"] == "1600" && root.attributes["height"] == "1040" && root.attributes["viewBox"] == "0 0 1600 1040"

  counts = {
    cards: REXML::XPath.match(doc, "//*[contains(concat(' ', normalize-space(@class), ' '), ' card ')]").size,
    participants: REXML::XPath.match(doc, "//*[contains(concat(' ', normalize-space(@class), ' '), ' participant ')]").size,
    headers: REXML::XPath.match(doc, "//*[contains(concat(' ', normalize-space(@class), ' '), ' header ')]").size,
    lifelines: REXML::XPath.match(doc, "//*[contains(concat(' ', normalize-space(@class), ' '), ' lifeline ')]").size,
    messages: REXML::XPath.match(doc, "//*[contains(concat(' ', normalize-space(@class), ' '), ' message ')]").size,
    numbered_labels: REXML::XPath.match(doc, "//*[contains(concat(' ', normalize-space(@class), ' '), ' num ')]").count { |node| node.text.to_s.strip.match?(/\A\d+\z/) },
    activations: REXML::XPath.match(doc, "//*[contains(concat(' ', normalize-space(@class), ' '), ' activation ')]").size,
    connectors: REXML::XPath.match(doc, "//*[contains(concat(' ', normalize-space(@class), ' '), ' connector ')]").size,
    frames: REXML::XPath.match(doc, "//*[contains(concat(' ', normalize-space(@class), ' '), ' chronological-frame ')]").size,
    markers: REXML::XPath.match(doc, "//marker[@markerUnits='userSpaceOnUse']").size
  }
  expected.each do |key, min|
    failures << "#{rel}: #{key}=#{counts[key]}, expected >= #{min}" if counts[key] < min
  end

  REXML::XPath.each(doc, "//marker") do |marker|
    size = marker.attributes["markerWidth"].to_i
    failures << "#{rel}: marker #{marker.attributes['id']} must be 14x14 or 16x16" unless [14, 16].include?(size) && marker.attributes["markerHeight"].to_i == size
  end
  REXML::XPath.each(doc, "//*[contains(concat(' ', normalize-space(@class), ' '), ' connector ')]") do |connector|
    width = connector.attributes["stroke-width"].to_f
    failures << "#{rel}: connector width #{width} < 4" if width < 4
    failures << "#{rel}: connector missing data-source/target" unless connector.attributes["data-source"] && connector.attributes["data-target"]
    failures << "#{rel}: diagonal connector command found" if connector.attributes["d"].to_s.match?(/\bL\s*\d+(?:\.\d+)?[ ,]+\d/)
  end
  if rel.end_with?("ddd-cqrs-sequence")
    REXML::XPath.each(doc, "//*[contains(concat(' ', normalize-space(@class), ' '), ' message ')]//*[contains(concat(' ', normalize-space(@class), ' '), ' connector ')]") do |connector|
      failures << "#{rel}: sequence connector must use a 16x16 marker" unless connector.attributes["marker-end"].to_s.end_with?("-16)")
    end
  end

  bytes = File.binread(png, 24)
  unless bytes.start_with?("\x89PNG".b)
    failures << "#{rel}: invalid PNG signature"
  else
    width, height = bytes[16, 8].unpack("NN")
    failures << "#{rel}: PNG is #{width}x#{height}, expected 3200x2080" unless [width, height] == [3200, 2080]
  end

  puts "#{rel}: #{counts.map { |k, v| "#{k}=#{v}" }.join(' ')}"
end

if failures.empty?
  puts "diagram-validation: failures=0 assets=#{ASSETS.size}"
else
  warn failures.join("\n")
  abort "diagram-validation: failures=#{failures.size}"
end
