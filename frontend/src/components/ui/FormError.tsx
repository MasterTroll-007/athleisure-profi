export default function FormError({ message }: { message: string | null }) {
  if (!message) return null
  return (
    <div className="mb-6 rounded-lg border border-red-300/25 bg-red-300/12 p-4 text-sm text-red-100 dark:bg-red-300/12 dark:text-red-100">
      {message}
    </div>
  )
}
