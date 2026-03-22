import { isAxiosError } from 'axios'

export function getApiErrorMessage(err: unknown, fallback: string): string {
  if (isAxiosError(err)) {
    return err.response?.data?.message ?? err.response?.data?.error ?? fallback
  }
  return fallback
}
