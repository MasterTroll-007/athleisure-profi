export default function FormError({ message }: { message: string | null }) {
  if (!message) return null
  return (
    <div className="mb-6 p-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg text-red-600 dark:text-red-400 text-sm">
      {message}
    </div>
  )
}
