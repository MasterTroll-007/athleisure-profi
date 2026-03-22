import { Eye, EyeOff } from 'lucide-react'

interface PasswordToggleButtonProps {
  visible: boolean
  onToggle: () => void
}

export default function PasswordToggleButton({ visible, onToggle }: PasswordToggleButtonProps) {
  return (
    <button
      type="button"
      onMouseDown={(e) => e.preventDefault()}
      onClick={onToggle}
      aria-label={visible ? 'Hide password' : 'Show password'}
      className="text-neutral-400 hover:text-neutral-600 dark:hover:text-neutral-300"
    >
      {visible ? <EyeOff size={18} /> : <Eye size={18} />}
    </button>
  )
}
