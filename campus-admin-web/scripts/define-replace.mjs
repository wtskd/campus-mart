/**
 * 极简 define 替换器（esbuild fallback）
 *
 * 模拟 esbuild 的 define 语义：只在「代码」区域替换标识符表达式，
 * 跳过字符串、注释、模板字符串原文。支持模板字符串 ${...} 内的表达式。
 *
 * @param {string} code 源码
 * @param {Record<string, string>} define 形如 { 'process.env.NODE_ENV': '"production"' } 的替换表（值本身就是 JS 表达式）
 * @returns {string}
 */
export function replaceDefines(code, define) {
  const keys = Object.keys(define || {}).sort((a, b) => b.length - a.length)
  if (!keys.length) return code

  const n = code.length
  let out = ''
  let i = 0

  // 状态栈：code / expr / template / single / double
  // expr 记录花括号深度，用于定位模板字符串 ${...} 的结束位置
  const stack = [{ mode: 'code', depth: 0 }]
  const top = () => stack[stack.length - 1]

  while (i < n) {
    const frame = top()

    if (frame.mode === 'code' || frame.mode === 'expr') {
      // ---- 尝试 define 替换 ----
      let replaced = false
      for (const key of keys) {
        if (!code.startsWith(key, i)) continue
        const prev = i > 0 ? code[i - 1] : ''
        const next = code[i + key.length] || ''
        // 避免替换到更长标识符的一部分（如 my_process.env / process.envX）
        if (prev && /[\w$.]/.test(prev)) continue
        if (next && /[\w$]/.test(next)) continue
        out += define[key]
        i += key.length
        replaced = true
        break
      }
      if (replaced) continue

      const ch = code[i]
      const ch2 = code[i + 1]

      // 注释
      if (ch === '/' && ch2 === '/') {
        const j = code.indexOf('\n', i)
        if (j === -1) {
          out += code.slice(i)
          break
        }
        out += code.slice(i, j)
        i = j
        continue
      }
      if (ch === '/' && ch2 === '*') {
        const j = code.indexOf('*/', i + 2)
        if (j === -1) {
          out += code.slice(i)
          break
        }
        out += code.slice(i, j + 2)
        i = j + 2
        continue
      }

      // 字符串 / 模板字符串
      if (ch === "'") {
        stack.push({ mode: 'single', depth: 0 })
        out += ch
        i++
        continue
      }
      if (ch === '"') {
        stack.push({ mode: 'double', depth: 0 })
        out += ch
        i++
        continue
      }
      if (ch === '`') {
        stack.push({ mode: 'template', depth: 0 })
        out += ch
        i++
        continue
      }

      // ${...} 表达式的边界
      if (frame.mode === 'expr') {
        if (ch === '{') frame.depth++
        else if (ch === '}') {
          frame.depth--
          if (frame.depth === 0) {
            stack.pop()
            out += ch
            i++
            continue
          }
        }
      }

      out += ch
      i++
      continue
    }

    if (frame.mode === 'single' || frame.mode === 'double') {
      const quote = frame.mode === 'single' ? "'" : '"'
      const ch = code[i]
      if (ch === '\\') {
        out += code.slice(i, i + 2)
        i += 2
        continue
      }
      out += ch
      i++
      if (ch === quote) stack.pop()
      continue
    }

    // ---- 模板字符串原文 ----
    const ch = code[i]
    if (ch === '\\') {
      out += code.slice(i, i + 2)
      i += 2
      continue
    }
    if (ch === '`') {
      out += ch
      i++
      stack.pop()
      continue
    }
    if (ch === '$' && code[i + 1] === '{') {
      out += '${'
      i += 2
      stack.push({ mode: 'expr', depth: 1 })
      continue
    }
    out += ch
    i++
  }

  return out
}
