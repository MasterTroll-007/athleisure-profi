import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import cs from './cs.json'
import en from './en.json'
import sk from './sk.json'

i18n.use(initReactI18next).init({
  resources: {
    cs: { translation: cs },
    en: { translation: en },
    sk: { translation: sk },
  },
  lng: localStorage.getItem('locale') || 'cs',
  fallbackLng: 'cs',
  interpolation: {
    // Enable escaping for security (React handles JSX escaping, but this adds defense-in-depth)
    escapeValue: true,
  },
})

export default i18n
