#!/usr/bin/env python3
"""
Parse and compare HexaGlue benchmark results (v2 vs v3).

Usage:
    python parse-results.py
    python parse-results.py <v2-file> <v3-file>
"""
import json
import os
import sys

# Get the directory where this script is located
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))

def parse_results(filepath):
    with open(filepath, 'r') as f:
        data = json.load(f)

    results = {}
    for entry in data:
        benchmark = entry['benchmark'].split('.')[-1]
        score = entry['primaryMetric']['score']
        error = entry['primaryMetric']['scoreError']
        results[benchmark] = {'score': score, 'error': error}
    return results

def main():
    # Default to files in the same directory as this script
    if len(sys.argv) == 3:
        v2_file = sys.argv[1]
        v3_file = sys.argv[2]
    else:
        v2_file = os.path.join(SCRIPT_DIR, 'results-v2.json')
        v3_file = os.path.join(SCRIPT_DIR, 'results-v3.json')
    
    v2 = parse_results(v2_file)
    v3 = parse_results(v3_file)
    
    print("=" * 80)
    print("HexaGlue Performance Benchmark Comparison: v2 vs v3")
    print("=" * 80)
    print()
    
    categories = {}
    for key in v2.keys():
        if 'parse' in key.lower():
            cat = 'Parsing'
        elif 'graph' in key.lower():
            cat = 'Graph Building'
        elif 'classify' in key.lower():
            cat = 'Classification'
        elif 'analyze' in key.lower():
            cat = 'End-to-End'
        else:
            cat = 'Other'
        
        if cat not in categories:
            categories[cat] = []
        categories[cat].append(key)
    
    for cat, benchmarks in categories.items():
        print(f"\n## {cat}\n")
        print(f"{'Benchmark':<25} {'v2 (ms)':<15} {'v3 (ms)':<15} {'Diff (%)':<12} {'Status'}")
        print("-" * 75)
        
        for bench in sorted(benchmarks):
            if bench in v2 and bench in v3:
                v2_score = v2[bench]['score']
                v3_score = v3[bench]['score']
                diff = ((v3_score - v2_score) / v2_score) * 100
                
                if diff <= -5:
                    status = "IMPROVED"
                elif diff <= 10:
                    status = "OK"
                elif diff <= 20:
                    status = "WARN"
                else:
                    status = "REGRESSION"
                
                print(f"{bench:<25} {v2_score:<15.3f} {v3_score:<15.3f} {diff:+.1f}%{'':<5} {status}")
    
    print()
    print("=" * 80)
    print("Legend: IMPROVED (<-5%), OK (-5% to +10%), WARN (+10% to +20%), REGRESSION (>+20%)")
    print("=" * 80)

if __name__ == '__main__':
    main()
