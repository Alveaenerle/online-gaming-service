import React from "react";
import { motion, AnimatePresence } from "framer-motion";
import { AlertTriangle, X, LogOut, DoorOpen } from "lucide-react";

interface ConfirmationModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  title?: string;
  message?: string;
  confirmText?: string;
  cancelText?: string;
  variant?: 'danger' | 'warning' | 'info';
  isLoading?: boolean;
}

export const ConfirmationModal: React.FC<ConfirmationModalProps> = ({
  isOpen,
  onClose,
  onConfirm,
  title = "Confirm Action",
  message = "Are you sure you want to proceed?",
  confirmText = "Confirm",
  cancelText = "Cancel",
  variant = 'warning',
  isLoading = false,
}) => {
  const variantStyles = {
    danger: {
      iconBg: 'bg-red-500/20',
      iconColor: 'text-red-400',
      buttonBg: 'bg-red-500 hover:bg-red-600',
      borderColor: 'border-red-500/30',
      gradientFrom: 'from-red-500/10',
    },
    warning: {
      iconBg: 'bg-orange-500/20',
      iconColor: 'text-orange-400',
      buttonBg: 'bg-orange-500 hover:bg-orange-600',
      borderColor: 'border-orange-500/30',
      gradientFrom: 'from-orange-500/10',
    },
    info: {
      iconBg: 'bg-blue-500/20',
      iconColor: 'text-blue-400',
      buttonBg: 'bg-blue-500 hover:bg-blue-600',
      borderColor: 'border-blue-500/30',
      gradientFrom: 'from-blue-500/10',
    },
  };

  const styles = variantStyles[variant];

  return (
    <AnimatePresence>
      {isOpen && (
        <>
          {/* Backdrop */}
          <motion.div
            className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
          />

          {/* Modal */}
          <motion.div
            className="fixed inset-0 z-50 flex items-center justify-center p-4"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
          >
            <motion.div
              initial={{ scale: 0.9, opacity: 0, y: 20 }}
              animate={{ scale: 1, opacity: 1, y: 0 }}
              exit={{ scale: 0.9, opacity: 0, y: 20 }}
              transition={{ type: "spring", stiffness: 300, damping: 25 }}
              className={`relative w-full max-w-md rounded-2xl border ${styles.borderColor}
                         bg-gradient-to-br ${styles.gradientFrom} to-purple-500/10
                         backdrop-blur-xl shadow-2xl shadow-purple-900/30`}
              onClick={(e) => e.stopPropagation()}
            >
              {/* Close button */}
              <button
                onClick={onClose}
                className="absolute top-4 right-4 p-2 rounded-lg text-gray-400 hover:text-white hover:bg-white/10 transition"
              >
                <X size={18} />
              </button>

              <div className="p-6">
                {/* Icon */}
                <div className="flex justify-center mb-4">
                  <div className={`p-4 rounded-full ${styles.iconBg}`}>
                    <DoorOpen className={`w-8 h-8 ${styles.iconColor}`} />
                  </div>
                </div>

                {/* Title */}
                <h2 className="text-xl font-bold text-white text-center mb-2">
                  {title}
                </h2>

                {/* Message */}
                <p className="text-gray-400 text-center mb-6">
                  {message}
                </p>

                {/* Actions */}
                <div className="flex gap-3">
                  <button
                    onClick={onClose}
                    disabled={isLoading}
                    className="flex-1 px-4 py-3 rounded-xl
                             bg-white/5 border border-white/10
                             text-gray-300 font-medium
                             hover:bg-white/10 hover:border-white/20
                             transition disabled:opacity-50"
                  >
                    {cancelText}
                  </button>
                  <button
                    onClick={() => {
                      onConfirm();
                      onClose();
                    }}
                    disabled={isLoading}
                    className={`flex-1 px-4 py-3 rounded-xl
                              ${styles.buttonBg}
                              text-white font-medium
                              transition disabled:opacity-50
                              flex items-center justify-center gap-2`}
                  >
                    {isLoading ? (
                      <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                    ) : (
                      <>
                        <LogOut size={18} />
                        {confirmText}
                      </>
                    )}
                  </button>
                </div>
              </div>
            </motion.div>
          </motion.div>
        </>
      )}
    </AnimatePresence>
  );
};

export default ConfirmationModal;
