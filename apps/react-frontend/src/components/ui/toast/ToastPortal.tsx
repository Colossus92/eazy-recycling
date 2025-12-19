import { useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { ToastContainer } from 'react-toastify';

/**
 * ToastPortal renders the ToastContainer into a portal that is appended
 * to the end of document.body. This ensures toasts appear above HeadlessUI
 * dialogs which use their own portal (headlessui-portal-root).
 * 
 * DOM order matters for stacking contexts - by appending our toast container
 * after HeadlessUI's portal, toasts will always appear on top.
 */
export const ToastPortal = () => {
  const [portalElement, setPortalElement] = useState<HTMLElement | null>(null);

  useEffect(() => {
    const element = document.createElement('div');
    element.id = 'toast-portal-root';
    document.body.appendChild(element);
    setPortalElement(element);

    return () => {
      document.body.removeChild(element);
    };
  }, []);

  if (!portalElement) return null;

  return createPortal(
    <ToastContainer style={{ zIndex: 99999 }} />,
    portalElement
  );
};
