# License Compliance Report

## Executive Summary

**Scan Date**: 2025-10-02
**Tool**: license-checker v25.0.1
**Scope**: Production dependencies only (website)
**Total Packages Scanned**: 391 packages

### Compliance Status: PASS

- **Prohibited Licenses Found**: 0
- **Compliance Level**: 100%
- **Risk Level**: LOW

## License Distribution

| License Type | Count | Status | Notes |
|--------------|-------|--------|-------|
| MIT | 337 | Approved | Permissive, commercial-friendly |
| ISC | 24 | Approved | Permissive, equivalent to MIT |
| Apache-2.0 | 10 | Approved | Permissive, patent grant included |
| BSD-3-Clause | 4 | Approved | Permissive, with attribution clause |
| BSD-2-Clause | 4 | Approved | Permissive, simplified BSD |
| BlueOak-1.0.0 | 3 | Approved | Modern permissive license |
| MPL-2.0 | 2 | Approved | Permissive, file-level copyleft |
| LGPL-3.0-or-later | 1 | Approved | Library GPL (linking permitted) |
| Python-2.0 | 1 | Approved | Permissive, historical Python license |
| CC-BY-4.0 | 1 | Approved | Creative Commons Attribution |
| CC0-1.0 | 1 | Approved | Public domain dedication |
| 0BSD | 1 | Approved | Zero-clause BSD (public domain) |
| (MIT OR CC0-1.0) | 1 | Approved | Dual-licensed (both permissive) |
| MIT* | 1 | Approved | MIT with minor variation |

**Total**: 391 packages across 14 license types

## Prohibited Licenses Check

The following license types are prohibited by SmilePile policy:

- **GPL** (GNU General Public License) - Strong copyleft
- **AGPL** (Affero GPL) - Network copyleft
- **SSPL** (Server Side Public License) - MongoDB license

### Scan Result

```bash
license-checker --failOn 'GPL;AGPL;SSPL' --production --excludePrivatePackages
```

**Result**: NO prohibited licenses detected

All dependencies use permissive or weak copyleft licenses compatible with commercial use.

## Notable Dependencies

### LGPL-3.0-or-later License

One dependency uses LGPL-3.0-or-later:
- **Package**: `@img/sharp-libvips-darwin-arm64@1.2.3`
- **Purpose**: Image processing library (native bindings for Sharp)
- **Compliance**: LGPL permits dynamic linking without copyleft obligations
- **Risk**: LOW - Used as compiled library, not modified
- **Action**: No action required (LGPL is acceptable for libraries)

### Apache-2.0 Dependencies (10 packages)

Apache-2.0 license includes explicit patent grant, providing additional protection:
- Includes express patent license from contributors
- Compatible with commercial use
- No restrictions on use, modification, or distribution

## Verification

### Running Compliance Checks

**Quick Summary**:
```bash
cd website
npm run licenses:check
```

**Full Compliance Check**:
```bash
cd website
npm run licenses:all
```

This will:
1. Generate CSV report at `docs/security/licenses.csv`
2. Verify no prohibited licenses (GPL/AGPL/SSPL)
3. Exit with code 0 if compliant, code 1 if violations found

**Manual Verification**:
```bash
cd website
npm run licenses:verify
```

## Recommendations

### For Developers

1. **Before adding dependencies**: Run `npm run licenses:verify` after `npm install`
2. **Review license compatibility**: Check new packages don't introduce GPL/AGPL
3. **Use npm scripts**: Automated checks prevent accidental violations

### For Dependency Updates

When Dependabot creates PR for dependency updates:

1. Run license compliance check: `npm run licenses:all`
2. Review any license changes in the PR
3. If prohibited license introduced, find alternative package
4. Document any exceptions (none currently needed)

### Ongoing Monitoring

- **Frequency**: Run license check on every dependency change
- **Automation**: Consider adding `licenses:verify` to CI/CD pipeline
- **Documentation**: Update this report when license distribution changes significantly

## Exceptions

### Current Exceptions

**None** - All current dependencies use approved licenses.

### Exception Process

If a GPL/AGPL dependency is deemed necessary:

1. Document business justification
2. Review legal implications with counsel
3. Explore alternative packages
4. If no alternative exists, document exception in separate file
5. Isolate GPL code (separate process, API boundary)
6. Update allowlist in package.json scripts

## Report Generation

This report was generated using:

```bash
cd /Users/adamstack/SmilePile/website
license-checker --summary --production --excludePrivatePackages
license-checker --csv --out ../docs/security/licenses.csv --production --excludePrivatePackages
license-checker --failOn 'GPL;AGPL;SSPL' --production --excludePrivatePackages
```

**CSV Report Location**: `/Users/adamstack/SmilePile/docs/security/licenses.csv`

## Next Steps

1. Add `licenses:verify` to CI/CD pipeline (recommended)
2. Review license compliance quarterly
3. Update this report when significant changes occur
4. Educate team on license compliance best practices

## References

- License Checker: https://github.com/davglass/license-checker
- Open Source Licenses: https://choosealicense.com/
- SPDX License List: https://spdx.org/licenses/
- SmilePile License Scripts: `website/package.json` (scripts section)

## Compliance History

| Date | Packages | Prohibited | Status | Notes |
|------|----------|------------|--------|-------|
| 2025-10-02 | 391 | 0 | PASS | Initial compliance scan, all dependencies approved |

---

**Last Updated**: 2025-10-02
**Next Review**: 2026-01-02 (quarterly)
