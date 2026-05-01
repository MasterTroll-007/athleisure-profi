import { describe, expect, it } from 'vitest'
import { readdirSync, readFileSync, statSync } from 'node:fs'
import { join } from 'node:path'
import cs from './cs.json'
import en from './en.json'

const pluralSuffixes = ['zero', 'one', 'two', 'few', 'many', 'other'] as const
const pluralSuffixPattern = new RegExp(`_(${pluralSuffixes.join('|')})$`)
const sourceExtensions = new Set(['.ts', '.tsx'])
const ignoredDirs = new Set(['node_modules', 'dist', 'coverage', 'e2e'])

type Dictionary = Record<string, unknown>

function flattenKeys(value: Dictionary, prefix = ''): string[] {
  return Object.entries(value).flatMap(([key, nestedValue]) => {
    const path = prefix ? `${prefix}.${key}` : key
    if (nestedValue && typeof nestedValue === 'object' && !Array.isArray(nestedValue)) {
      return flattenKeys(nestedValue as Dictionary, path)
    }
    return path
  })
}

function walkFiles(dir: string): string[] {
  return readdirSync(dir).flatMap((entry) => {
    if (ignoredDirs.has(entry)) return []

    const fullPath = join(dir, entry)
    const stats = statSync(fullPath)

    if (stats.isDirectory()) return walkFiles(fullPath)
    if (![...sourceExtensions].some((extension) => fullPath.endsWith(extension))) return []
    if (fullPath.endsWith('.test.ts') || fullPath.endsWith('.test.tsx')) return []
    return fullPath
  })
}

function collectUsedTranslationKeys() {
  const sourceRoot = join(process.cwd(), 'src')
  const files = walkFiles(sourceRoot)
  const keys = new Set<string>()

  const patterns = [
    /\bt\s*\(\s*['"`]([A-Za-z0-9_.-]+)['"`]/g,
    /\bi18nKey\s*=\s*['"`]([A-Za-z0-9_.-]+)['"`]/g,
    /\bi18nKey\s*=\s*\{\s*['"`]([A-Za-z0-9_.-]+)['"`]\s*\}/g,
  ]

  for (const file of files) {
    const contents = readFileSync(file, 'utf8')
    for (const pattern of patterns) {
      for (const match of contents.matchAll(pattern)) {
        keys.add(match[1])
      }
    }
  }

  return [...keys].sort()
}

function hasTranslation(flatKeys: Set<string>, key: string) {
  if (flatKeys.has(key)) return true
  return pluralSuffixes.some((suffix) => flatKeys.has(`${key}_${suffix}`))
}

function baseTranslationKeys(flatKeys: Set<string>) {
  return new Set([...flatKeys].map((key) => key.replace(pluralSuffixPattern, '')))
}

describe('i18n dictionaries', () => {
  const csKeys = new Set(flattenKeys(cs))
  const enKeys = new Set(flattenKeys(en))

  it('contain every translation key used by frontend source', () => {
    const usedKeys = collectUsedTranslationKeys()
    const missingCs = usedKeys.filter((key) => !hasTranslation(csKeys, key))
    const missingEn = usedKeys.filter((key) => !hasTranslation(enKeys, key))

    expect({ cs: missingCs, en: missingEn }).toEqual({ cs: [], en: [] })
  })

  it('keep Czech and English dictionaries in sync', () => {
    const csBaseKeys = baseTranslationKeys(csKeys)
    const enBaseKeys = baseTranslationKeys(enKeys)
    const onlyCs = [...csBaseKeys].filter((key) => !enBaseKeys.has(key)).sort()
    const onlyEn = [...enBaseKeys].filter((key) => !csBaseKeys.has(key)).sort()

    expect({ onlyCs, onlyEn }).toEqual({ onlyCs: [], onlyEn: [] })
  })
})
