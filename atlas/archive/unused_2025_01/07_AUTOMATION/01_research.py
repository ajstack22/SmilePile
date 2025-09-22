#!/usr/bin/env python3
"""
Atlas Research Script - Designed to be called by Claude Orchestrator
This script performs actual research tasks and returns results to Claude
"""

import sys
import json
import os
import subprocess
from pathlib import Path
from datetime import datetime

class ResearchAgent:
    """
    A research agent that Claude can invoke to perform specific research tasks
    """

    def __init__(self):
        self.state_dir = Path('.atlas/research')
        self.state_dir.mkdir(parents=True, exist_ok=True)
        self.results = {}

    def search_codebase(self, pattern, file_type="*"):
        """
        Search codebase for patterns
        Returns: List of matches with file and line number
        """
        results = []
        try:
            # Use ripgrep for fast searching
            cmd = f"rg '{pattern}' --type-add 'custom:{file_type}' -t custom --json"
            output = subprocess.run(cmd, shell=True, capture_output=True, text=True)

            for line in output.stdout.split('\n'):
                if line:
                    try:
                        data = json.loads(line)
                        if data.get('type') == 'match':
                            results.append({
                                'file': data['data']['path']['text'],
                                'line': data['data']['line_number'],
                                'text': data['data']['lines']['text'].strip()
                            })
                    except:
                        continue
        except Exception as e:
            results.append({'error': str(e)})

        return results

    def analyze_structure(self, directory="."):
        """
        Analyze project structure
        Returns: Dictionary of project organization
        """
        structure = {
            'languages': {},
            'frameworks': [],
            'config_files': [],
            'test_framework': None,
            'build_system': None
        }

        # Detect languages by extension
        extensions = {}
        for root, dirs, files in os.walk(directory):
            # Skip hidden and node_modules
            dirs[:] = [d for d in dirs if not d.startswith('.') and d != 'node_modules']

            for file in files:
                ext = Path(file).suffix
                if ext:
                    extensions[ext] = extensions.get(ext, 0) + 1

        # Map extensions to languages
        lang_map = {
            '.py': 'Python',
            '.js': 'JavaScript',
            '.ts': 'TypeScript',
            '.java': 'Java',
            '.go': 'Go',
            '.rs': 'Rust',
            '.cpp': 'C++',
            '.c': 'C'
        }

        for ext, count in extensions.items():
            if ext in lang_map:
                structure['languages'][lang_map[ext]] = count

        # Detect frameworks and tools
        if os.path.exists('package.json'):
            structure['frameworks'].append('Node.js')
            with open('package.json', 'r') as f:
                pkg = json.load(f)
                deps = list(pkg.get('dependencies', {}).keys())
                if 'react' in deps:
                    structure['frameworks'].append('React')
                if 'vue' in deps:
                    structure['frameworks'].append('Vue')
                if 'express' in deps:
                    structure['frameworks'].append('Express')
                if 'jest' in deps or 'mocha' in deps:
                    structure['test_framework'] = 'Jest/Mocha'

        if os.path.exists('requirements.txt') or os.path.exists('Pipfile'):
            structure['frameworks'].append('Python')
            if os.path.exists('manage.py'):
                structure['frameworks'].append('Django')

        if os.path.exists('go.mod'):
            structure['frameworks'].append('Go Modules')

        if os.path.exists('Cargo.toml'):
            structure['frameworks'].append('Rust/Cargo')

        # Detect build system
        if os.path.exists('Makefile'):
            structure['build_system'] = 'Make'
        elif os.path.exists('webpack.config.js'):
            structure['build_system'] = 'Webpack'
        elif os.path.exists('vite.config.js'):
            structure['build_system'] = 'Vite'

        return structure

    def find_similar_implementations(self, pattern):
        """
        Find similar patterns in codebase
        Returns: List of similar implementations
        """
        # This would search for similar patterns
        results = self.search_codebase(pattern)

        # Group by file
        by_file = {}
        for match in results:
            if 'file' in match:
                file = match['file']
                if file not in by_file:
                    by_file[file] = []
                by_file[file].append(match)

        return {
            'total_matches': len(results),
            'files_affected': len(by_file),
            'matches_by_file': by_file
        }

    def research_dependencies(self):
        """
        Analyze project dependencies
        Returns: Dependency information
        """
        deps = {
            'direct': [],
            'dev': [],
            'security_issues': [],
            'outdated': []
        }

        # Check Node.js dependencies
        if os.path.exists('package.json'):
            with open('package.json', 'r') as f:
                pkg = json.load(f)
                deps['direct'] = list(pkg.get('dependencies', {}).keys())
                deps['dev'] = list(pkg.get('devDependencies', {}).keys())

        # Check Python dependencies
        if os.path.exists('requirements.txt'):
            with open('requirements.txt', 'r') as f:
                deps['direct'].extend([
                    line.split('==')[0].strip()
                    for line in f.readlines()
                    if line.strip() and not line.startswith('#')
                ])

        return deps

    def save_research_results(self, topic, results):
        """Save research results for future reference"""
        timestamp = datetime.now().isoformat()
        filename = f"research_{topic.replace(' ', '_')}_{timestamp}.json"
        filepath = self.state_dir / filename

        with open(filepath, 'w') as f:
            json.dump({
                'topic': topic,
                'timestamp': timestamp,
                'results': results
            }, f, indent=2)

        return str(filepath)

    def execute_research(self, topic, research_type='full'):
        """
        Main research execution - Called by Claude

        Args:
            topic: What to research
            research_type: 'full', 'quick', 'structural', 'dependencies'

        Returns:
            JSON string with research results
        """
        results = {
            'topic': topic,
            'type': research_type,
            'timestamp': datetime.now().isoformat(),
            'findings': {}
        }

        if research_type in ['full', 'structural']:
            results['findings']['structure'] = self.analyze_structure()

        if research_type in ['full', 'dependencies']:
            results['findings']['dependencies'] = self.research_dependencies()

        if research_type == 'full':
            # Search for topic-related patterns
            results['findings']['code_references'] = self.search_codebase(topic)
            results['findings']['similar_patterns'] = self.find_similar_implementations(topic)

        # Save results
        saved_path = self.save_research_results(topic, results)
        results['saved_to'] = saved_path

        return json.dumps(results, indent=2)

def main():
    """
    Entry point when Claude runs this script

    Usage:
        python 01_research.py --topic "authentication" --type full
        python 01_research.py --analyze-structure
        python 01_research.py --search "pattern" --file-type "*.js"
    """
    agent = ResearchAgent()

    # Parse command line arguments
    args = sys.argv[1:]

    if '--topic' in args:
        idx = args.index('--topic')
        topic = args[idx + 1] if idx + 1 < len(args) else 'general'

        research_type = 'full'
        if '--type' in args:
            type_idx = args.index('--type')
            research_type = args[type_idx + 1] if type_idx + 1 < len(args) else 'full'

        # Execute research and print results for Claude to parse
        results = agent.execute_research(topic, research_type)
        print(results)

    elif '--analyze-structure' in args:
        structure = agent.analyze_structure()
        print(json.dumps(structure, indent=2))

    elif '--search' in args:
        idx = args.index('--search')
        pattern = args[idx + 1] if idx + 1 < len(args) else ''

        file_type = "*"
        if '--file-type' in args:
            type_idx = args.index('--file-type')
            file_type = args[type_idx + 1] if type_idx + 1 < len(args) else "*"

        results = agent.search_codebase(pattern, file_type)
        print(json.dumps(results, indent=2))

    elif '--dependencies' in args:
        deps = agent.research_dependencies()
        print(json.dumps(deps, indent=2))

    else:
        # Return usage information
        usage = {
            'script': '01_research.py',
            'description': 'Research agent for codebase analysis',
            'commands': [
                {
                    'command': '--topic [topic] --type [full|quick|structural]',
                    'description': 'Research a specific topic'
                },
                {
                    'command': '--analyze-structure',
                    'description': 'Analyze project structure'
                },
                {
                    'command': '--search [pattern] --file-type [pattern]',
                    'description': 'Search codebase for patterns'
                },
                {
                    'command': '--dependencies',
                    'description': 'Analyze project dependencies'
                }
            ]
        }
        print(json.dumps(usage, indent=2))

if __name__ == "__main__":
    main()