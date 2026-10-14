#!/usr/bin/env python3
"""Converts the iOS Localizable.strings files into Android string resources.

Usage: python3 tools/convert_localization.py <localization_dir> <res_dir> <kotlin_dir>
"""
import os
import re
import sys

STRING_LINE = re.compile(r'^"((?:[^"\\]|\\.)*)"\s*=\s*"((?:[^"\\]|\\.)*)"\s*;')

UNESCAPE = {
    'n': '\n',
    't': '\t',
    'r': '\r',
    '"': '"',
    "'": "'",
    '\\': '\\',
}


def unescape(value):
    result = []
    index = 0
    while index < len(value):
        character = value[index]
        if character == '\\' and index + 1 < len(value):
            following = value[index + 1]
            result.append(UNESCAPE.get(following, following))
            index += 2
        else:
            result.append(character)
            index += 1
    return ''.join(result)


def parse_strings(path):
    entries = {}
    with open(path, encoding='utf-8') as handle:
        for line in handle:
            match = STRING_LINE.match(line.strip())
            if not match:
                continue
            entries[unescape(match.group(1))] = unescape(match.group(2))
    return entries


def android_format(value):
    counter = [0]

    def replace(match):
        token = match.group(1)
        if token == '%':
            return '%%'
        counter[0] += 1
        kind = 's' if token == '@' else 'd'
        return '%%%d$%s' % (counter[0], kind)

    return re.sub(r'%(@|d|%)', replace, value)


def android_escape(value):
    value = value.replace('\\', '\\\\')
    value = value.replace("'", "\\'")
    value = value.replace('"', '\\"')
    value = value.replace('\n', '\\n')
    value = value.replace('\t', '\\t')
    value = value.replace('\r', '\\r')
    value = value.replace('&', '&amp;')
    value = value.replace('<', '&lt;')
    value = value.replace('>', '&gt;')
    if value.startswith('@') or value.startswith('?'):
        value = '\\' + value
    return value


def resource_name(key):
    return key.replace('.', '_')


def write_strings(path, entries):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as handle:
        handle.write('<?xml version="1.0" encoding="utf-8"?>\n')
        handle.write('<!-- Generated from the iOS Localizable.strings. Do not edit by hand. -->\n')
        handle.write('<resources>\n')
        for key in sorted(entries):
            value = android_escape(android_format(entries[key]))
            attribute = ' translatable="false"' if key == 'app.name' else ''
            handle.write('    <string name="%s"%s>%s</string>\n' % (resource_name(key), attribute, value))
        handle.write('</resources>\n')


def main():
    localization_dir, res_dir, kotlin_dir = sys.argv[1:4]
    languages = {
        'en': 'values',
        'zh-Hans': 'values-zh-rCN',
        'zh-Hant': 'values-zh-rTW',
        'zh-HK': 'values-zh-rHK',
        'ja': 'values-ja',
        'ko': 'values-ko',
        'th': 'values-th',
        'id': 'values-in',
        'ms': 'values-ms',
    }
    reference = None
    keys = []
    for language, folder in languages.items():
        path = os.path.join(localization_dir, language + '.lproj', 'Localizable.strings')
        entries = parse_strings(path)
        if reference is None:
            reference = entries
            keys = sorted(entries)
        missing = set(reference) - set(entries)
        if missing:
            raise SystemExit('missing keys in %s: %s' % (language, sorted(missing)[:5]))
        if language == 'en':
            entries = dict(entries)
            entries['app.name'] = 'Oshicue Kit'
        write_strings(os.path.join(res_dir, folder, 'strings.xml'), entries)
        print('%s -> %s (%d strings)' % (language, folder, len(entries)))

    os.makedirs(kotlin_dir, exist_ok=True)
    with open(os.path.join(kotlin_dir, 'L10nKeys.kt'), 'w', encoding='utf-8') as handle:
        handle.write('package com.oshicue.kit.i18n\n\n')
        handle.write('import com.oshicue.kit.R\n\n')
        handle.write('/** Generated from the iOS Localizable.strings keys. Do not edit by hand. */\n')
        handle.write('internal val KEY_RESOURCES: Map<String, Int> = mapOf(\n')
        for key in keys:
            handle.write('    "%s" to R.string.%s,\n' % (key, resource_name(key)))
        handle.write(')\n')
    print('generated %d key mappings' % len(keys))


if __name__ == '__main__':
    main()
