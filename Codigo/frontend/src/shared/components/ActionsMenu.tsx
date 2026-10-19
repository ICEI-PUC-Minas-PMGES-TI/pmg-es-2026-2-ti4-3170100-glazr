import { useCallback, useEffect, useId, useLayoutEffect, useRef, useState, type KeyboardEvent, type ReactNode } from 'react'
import { createPortal } from 'react-dom'
import { ChevronDown } from 'lucide-react'
import './ActionsMenu.css'

export interface ActionsMenuItem {
  readonly label: string
  readonly onSelect: () => void
  readonly icon?: ReactNode
  readonly tone?: 'default' | 'danger'
}

interface ActionsMenuProps {
  readonly label: string
  readonly items: ReadonlyArray<ActionsMenuItem>
  readonly disabled?: boolean
}

interface MenuPosition {
  readonly top: number
  readonly left: number
}

const MENU_GAP_PX = 4

export function ActionsMenu({ label, items, disabled = false }: ActionsMenuProps) {
  const [requestedOpen, setOpen] = useState(false)
  const open = requestedOpen && !disabled
  const [position, setPosition] = useState<MenuPosition | null>(null)
  const triggerRef = useRef<HTMLButtonElement>(null)
  const menuRef = useRef<HTMLDivElement>(null)
  const menuId = useId()

  const close = useCallback((restoreFocus: boolean) => {
    setOpen(false)
    if (restoreFocus) triggerRef.current?.focus({ preventScroll: true })
  }, [])

  // O menu é renderizado no body para não ser cortado pela tabela (overflow e backdrop-filter das linhas).
  useLayoutEffect(() => {
    if (!open || !triggerRef.current || !menuRef.current) return
    const trigger = triggerRef.current.getBoundingClientRect()
    const menu = menuRef.current.getBoundingClientRect()
    const fitsBelow = trigger.bottom + MENU_GAP_PX + menu.height <= window.innerHeight
    const top = fitsBelow ? trigger.bottom + MENU_GAP_PX : trigger.top - MENU_GAP_PX - menu.height
    const left = Math.max(MENU_GAP_PX, Math.min(trigger.right - menu.width, window.innerWidth - menu.width - MENU_GAP_PX))
    setPosition({ top, left })
    menuRef.current.querySelector<HTMLButtonElement>('[role="menuitem"]')?.focus({ preventScroll: true })
  }, [open])

  useEffect(() => {
    if (!open) return
    function handlePointerDown(event: PointerEvent) {
      const target = event.target as Node
      if (!menuRef.current?.contains(target) && !triggerRef.current?.contains(target)) close(false)
    }
    function handleViewportChange() {
      close(false)
    }
    document.addEventListener('pointerdown', handlePointerDown)
    window.addEventListener('resize', handleViewportChange)
    window.addEventListener('scroll', handleViewportChange, true)
    return () => {
      document.removeEventListener('pointerdown', handlePointerDown)
      window.removeEventListener('resize', handleViewportChange)
      window.removeEventListener('scroll', handleViewportChange, true)
    }
  }, [open, close])


  function handleMenuKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    const menuItems = [...(menuRef.current?.querySelectorAll<HTMLButtonElement>('[role="menuitem"]') ?? [])]
    const currentIndex = menuItems.indexOf(document.activeElement as HTMLButtonElement)
    if (event.key === 'Escape') {
      event.preventDefault()
      close(true)
    } else if (event.key === 'Tab') {
      close(false)
    } else if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault()
      const step = event.key === 'ArrowDown' ? 1 : -1
      menuItems[(currentIndex + step + menuItems.length) % menuItems.length]?.focus({ preventScroll: true })
    } else if (event.key === 'Home' || event.key === 'End') {
      event.preventDefault()
      menuItems[event.key === 'Home' ? 0 : menuItems.length - 1]?.focus({ preventScroll: true })
    }
  }

  if (items.length === 0) return null

  return (
    <>
      <button
        ref={triggerRef}
        className="button button--secondary actions-menu__trigger"
        type="button"
        aria-haspopup="menu"
        aria-expanded={open}
        aria-controls={open ? menuId : undefined}
        aria-label={label}
        disabled={disabled}
        onClick={() => setOpen((current) => !current)}
      >
        Ações <ChevronDown size={16} aria-hidden="true" />
      </button>
      {open
        ? createPortal(
            <div
              ref={menuRef}
              id={menuId}
              className="actions-menu"
              role="menu"
              aria-label={label}
              style={{ top: position?.top ?? 0, left: position?.left ?? 0, visibility: position ? 'visible' : 'hidden' }}
              onKeyDown={handleMenuKeyDown}
            >
              {items.map((item) => (
                <button
                  key={item.label}
                  className={`actions-menu__item${item.tone === 'danger' ? ' actions-menu__item--danger' : ''}`}
                  type="button"
                  role="menuitem"
                  tabIndex={-1}
                  onClick={() => {
                    close(true)
                    item.onSelect()
                  }}
                >
                  {item.icon}
                  {item.label}
                </button>
              ))}
            </div>,
            document.body,
          )
        : null}
    </>
  )
}
