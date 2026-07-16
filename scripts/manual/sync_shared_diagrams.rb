#!/usr/bin/env ruby
# frozen_string_literal: true

require_relative "shared_diagram_contract"

root = File.expand_path("../..", __dir__)
inventory = File.join(root, "docs/manual/shared-diagrams.yaml")
contract = SharedDiagrams::Contract.new(root: root, inventory_path: inventory)

begin
  case ARGV
  when ["--write"]
    contract.sync!
    puts "shared-diagrams: synchronized selected=#{contract.entries.count(&:selected?)} active=#{contract.active_entries.size} target=#{contract.target_minor} stable=#{contract.stable_minor}"
  when ["--check"]
    failures = contract.errors
    if failures.empty?
      puts "shared-diagrams: failures=0 entries=#{contract.entries.size} selected=#{contract.entries.count(&:selected?)} active=#{contract.active_entries.size} deferred=#{contract.entries.count(&:deferred?)} target=#{contract.target_minor} stable=#{contract.stable_minor}"
    else
      warn failures.join("\n")
      exit 1
    end
  else
    warn "usage: sync_shared_diagrams.rb --write|--check"
    exit 2
  end
rescue SharedDiagrams::ContractError => error
  warn error.message
  exit 1
end
