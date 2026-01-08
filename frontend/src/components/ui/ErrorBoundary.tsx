import { Component, ErrorInfo, ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { AlertTriangle, RefreshCw, Home } from 'lucide-react'
import Button from './Button'
import Card from './Card'

interface ErrorBoundaryProps {
  children: ReactNode
  fallback?: ReactNode
}

interface ErrorBoundaryState {
  hasError: boolean
  error: Error | null
}

class ErrorBoundaryClass extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props)
    this.state = { hasError: false, error: null }
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error }
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('ErrorBoundary caught an error:', error, errorInfo)
  }

  handleRefresh = () => {
    this.setState({ hasError: false, error: null })
    window.location.reload()
  }

  handleGoHome = () => {
    this.setState({ hasError: false, error: null })
    window.location.href = '/'
  }

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback
      }

      return (
        <div className="min-h-screen flex items-center justify-center p-4 bg-neutral-50 dark:bg-dark-bg">
          <Card className="max-w-md w-full text-center">
            <div className="flex justify-center mb-4">
              <div className="w-16 h-16 rounded-full bg-red-100 dark:bg-red-900/30 flex items-center justify-center">
                <AlertTriangle size={32} className="text-red-500" />
              </div>
            </div>
            <h2 className="text-xl font-heading font-bold text-neutral-900 dark:text-white mb-2">
              Nastala chyba
            </h2>
            <p className="text-neutral-600 dark:text-neutral-400 mb-6">
              Omlouváme se, ale nastala neočekávaná chyba. Zkuste obnovit stránku nebo se vrátit na úvodní stránku.
            </p>
            <div className="flex gap-3 justify-center">
              <Button
                variant="secondary"
                onClick={this.handleRefresh}
                className="flex items-center gap-2"
              >
                <RefreshCw size={16} />
                Obnovit
              </Button>
              <Button
                variant="primary"
                onClick={this.handleGoHome}
                className="flex items-center gap-2"
              >
                <Home size={16} />
                Domů
              </Button>
            </div>
          </Card>
        </div>
      )
    }

    return this.props.children
  }
}

// Wrapper component with translations
export function ErrorBoundary({ children, fallback }: ErrorBoundaryProps) {
  return (
    <ErrorBoundaryClass fallback={fallback}>
      {children}
    </ErrorBoundaryClass>
  )
}

// Hook for using translation in the error fallback
export function ErrorFallback() {
  const { i18n } = useTranslation()
  const isCs = i18n.language === 'cs'

  const handleRefresh = () => {
    window.location.reload()
  }

  const handleGoHome = () => {
    window.location.href = '/'
  }

  return (
    <div className="min-h-screen flex items-center justify-center p-4 bg-neutral-50 dark:bg-dark-bg">
      <Card className="max-w-md w-full text-center">
        <div className="flex justify-center mb-4">
          <div className="w-16 h-16 rounded-full bg-red-100 dark:bg-red-900/30 flex items-center justify-center">
            <AlertTriangle size={32} className="text-red-500" />
          </div>
        </div>
        <h2 className="text-xl font-heading font-bold text-neutral-900 dark:text-white mb-2">
          {isCs ? 'Nastala chyba' : 'An error occurred'}
        </h2>
        <p className="text-neutral-600 dark:text-neutral-400 mb-6">
          {isCs
            ? 'Omlouváme se, ale nastala neočekávaná chyba. Zkuste obnovit stránku nebo se vrátit na úvodní stránku.'
            : 'We apologize, but an unexpected error occurred. Try refreshing the page or returning to the home page.'}
        </p>
        <div className="flex gap-3 justify-center">
          <Button
            variant="secondary"
            onClick={handleRefresh}
            className="flex items-center gap-2"
          >
            <RefreshCw size={16} />
            {isCs ? 'Obnovit' : 'Refresh'}
          </Button>
          <Button
            variant="primary"
            onClick={handleGoHome}
            className="flex items-center gap-2"
          >
            <Home size={16} />
            {isCs ? 'Domů' : 'Home'}
          </Button>
        </div>
      </Card>
    </div>
  )
}

export default ErrorBoundary
