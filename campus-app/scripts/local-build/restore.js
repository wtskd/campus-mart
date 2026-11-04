/**
 * 撤销 install-shim.js 对 node_modules 的本地补丁（恢复官方文件）
 * 用法：node scripts/local-build/restore.js
 */
const fs = require('fs')
const path = require('path')

const root = path.join(__dirname, '..')

function restoreEsbuild() {
  const libDir = path.join(root, 'node_modules', 'esbuild', 'lib')
  const main = path.join(libDir, 'main.js')
  const backup = path.join(libDir, 'main.real.js')
  if (fs.existsSync(backup)) {
    fs.copyFileSync(backup, main)
    console.log('[ok] esbuild/lib/main.js 已还原')
  } else {
    console.log('[skip] 没有 esbuild 备份')
  }
}

function restoreVite() {
  const chunkDir = path.join(root, 'node_modules', 'vite', 'dist', 'node', 'chunks')
  if (!fs.existsSync(chunkDir)) return
  fs.readdirSync(chunkDir)
    .filter((f) => f.endsWith('.js.bak'))
    .forEach((bak) => {
      const target = path.join(chunkDir, bak.replace(/\.bak$/, ''))
      fs.copyFileSync(path.join(chunkDir, bak), target)
      console.log('[ok] 已还原', bak.replace(/\.bak$/, ''))
    })
}

restoreEsbuild()
restoreVite()
console.log('\n完成：node_modules 已恢复官方版本。')
