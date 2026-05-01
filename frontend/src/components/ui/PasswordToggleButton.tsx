import { Eye, EyeOff } from 'lucide-react'
import { useTranslation } from 'react-i18next'

interface PasswordToggleButtonProps {
  visible: boolean
  onToggle: () => void
}

export default function PasswordToggleButton({ visible, onToggle }: PasswordToggleButtonProps) {
  const { t } = useTranslation()

  return (
    <button
      type="button"
      onMouseDown={(e) => e.preventDefault()}
      onClick={onToggle}
      aria-label={visible ? t('auth.hidePassword') : t('auth.showPassword')}
      className="text-neutral-400 hover:text-neutral-600 dark:hover:text-neutral-300"
    >
      {visible ? <EyeOff size={18} /> : <Eye size={18} />}
    </button>
  )
}
