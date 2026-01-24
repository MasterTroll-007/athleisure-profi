import { useState, useEffect, useRef, useCallback } from 'react'
import { adminApi } from '@/services/api'
import type { User } from '@/types/api'

export function useUserSearch() {
  const [searchQuery, setSearchQuery] = useState('')
  const [searchResults, setSearchResults] = useState<User[]>([])
  const [isSearching, setIsSearching] = useState(false)
  const [showUserSearch, setShowUserSearch] = useState(false)
  const [selectedUser, setSelectedUser] = useState<User | null>(null)
  const searchInputRef = useRef<HTMLInputElement>(null)

  // Debounced search effect
  useEffect(() => {
    if (searchQuery.length < 2) {
      setSearchResults([])
      return
    }

    const timeoutId = setTimeout(async () => {
      setIsSearching(true)
      try {
        const results = await adminApi.searchClients(searchQuery)
        setSearchResults(results)
      } catch {
        setSearchResults([])
      } finally {
        setIsSearching(false)
      }
    }, 300)

    return () => clearTimeout(timeoutId)
  }, [searchQuery])

  // Focus search input when modal opens
  useEffect(() => {
    if (showUserSearch && searchInputRef.current) {
      searchInputRef.current.focus()
    }
  }, [showUserSearch])

  const handleSelectUser = useCallback((user: User) => {
    setSelectedUser(user)
    setSearchQuery('')
    setSearchResults([])
  }, [])

  const openUserSearch = useCallback(() => {
    setShowUserSearch(true)
  }, [])

  const closeUserSearch = useCallback(() => {
    setShowUserSearch(false)
    setSearchQuery('')
    setSearchResults([])
  }, [])

  const clearSelectedUser = useCallback(() => {
    setSelectedUser(null)
  }, [])

  const reset = useCallback(() => {
    setSearchQuery('')
    setSearchResults([])
    setShowUserSearch(false)
    setSelectedUser(null)
  }, [])

  return {
    // State
    searchQuery,
    searchResults,
    isSearching,
    showUserSearch,
    selectedUser,
    searchInputRef,
    // Setters
    setSearchQuery,
    // Actions
    handleSelectUser,
    openUserSearch,
    closeUserSearch,
    clearSelectedUser,
    reset,
  }
}
