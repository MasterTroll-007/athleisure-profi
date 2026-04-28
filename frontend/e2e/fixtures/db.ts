import { spawn } from 'node:child_process'
import { readFile } from 'node:fs/promises'
import path from 'node:path'

function getRepoRoot(): string {
  return path.basename(process.cwd()) === 'frontend'
    ? path.resolve(process.cwd(), '..')
    : process.cwd()
}

export async function runE2eSql(sql: string): Promise<void> {
  const repoRoot = getRepoRoot()
  const composeFile = path.join(repoRoot, 'docker-compose.local.yml')
  const child = spawn(
    'docker',
    ['compose', '-f', composeFile, 'exec', '-T', 'db', 'psql', '-U', 'fitness', '-d', 'fitness', '-v', 'ON_ERROR_STOP=1'],
    { cwd: repoRoot }
  )

  let stdout = ''
  let stderr = ''
  child.stdout.on('data', (chunk: Buffer) => {
    stdout += chunk.toString()
  })
  child.stderr.on('data', (chunk: Buffer) => {
    stderr += chunk.toString()
  })

  const done = new Promise<void>((resolve, reject) => {
    child.on('error', reject)
    child.on('close', (code) => {
      if (code === 0) {
        resolve()
        return
      }
      reject(new Error(`E2E SQL failed with exit code ${code}\n${stdout}\n${stderr}`))
    })
  })

  child.stdin.end(sql)
  await done
}

export async function resetE2eData(): Promise<void> {
  const sqlPath = path.join(getRepoRoot(), 'test-data', 'e2e-reset.sql')
  const sql = await readFile(sqlPath, 'utf8')
  await runE2eSql(sql)
}

export async function activateE2eUser(email: string): Promise<void> {
  const safeEmail = email.replace(/'/g, "''")
  await runE2eSql(`
    UPDATE users
    SET email_verified = true, credits = 10, updated_at = NOW()
    WHERE email = '${safeEmail}';
  `)
}
