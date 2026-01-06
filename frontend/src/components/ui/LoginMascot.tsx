import { motion } from 'framer-motion'

interface LoginMascotProps {
  focusedField: 'email' | 'password' | null
  isPasswordVisible: boolean
}

export default function LoginMascot({ focusedField, isPasswordVisible }: LoginMascotProps) {
  const isCovering = focusedField === 'password' && !isPasswordVisible
  const isPeeking = focusedField === 'password' && isPasswordVisible

  return (
    <div className="flex justify-center mb-4">
      <div className="relative w-28 h-28">
        {/* Dog face */}
        <div className="w-28 h-24 bg-gradient-to-b from-amber-600 to-amber-700 rounded-[50%] shadow-lg relative overflow-visible">

          {/* Ears */}
          <motion.div
            className="absolute -top-3 -left-2 w-10 h-14 bg-gradient-to-b from-amber-700 to-amber-800 rounded-[50%] rotate-[-20deg] origin-bottom"
            animate={{ rotate: focusedField === 'email' ? -15 : -20 }}
            transition={{ duration: 0.3 }}
          />
          <motion.div
            className="absolute -top-3 -right-2 w-10 h-14 bg-gradient-to-b from-amber-700 to-amber-800 rounded-[50%] rotate-[20deg] origin-bottom"
            animate={{ rotate: focusedField === 'email' ? 15 : 20 }}
            transition={{ duration: 0.3 }}
          />

          {/* Inner ear */}
          <div className="absolute -top-1 left-0 w-6 h-8 bg-pink-300/40 rounded-[50%] rotate-[-20deg]" />
          <div className="absolute -top-1 right-0 w-6 h-8 bg-pink-300/40 rounded-[50%] rotate-[20deg]" />

          {/* Face lighter patch */}
          <div className="absolute top-6 left-1/2 -translate-x-1/2 w-20 h-16 bg-gradient-to-b from-amber-400 to-amber-500 rounded-[50%]" />

          {/* Eyes container */}
          <div className="absolute top-8 left-1/2 -translate-x-1/2 flex gap-6">
            {/* Left eye */}
            <motion.div
              className="relative"
              animate={{ y: focusedField === 'email' ? 3 : 0 }}
              transition={{ duration: 0.3 }}
            >
              <div className="w-5 h-5 bg-white rounded-full flex items-center justify-center shadow-inner border border-neutral-200">
                <motion.div
                  className="w-2.5 h-2.5 bg-neutral-800 rounded-full relative"
                  animate={{
                    y: focusedField === 'email' ? 1 : 0,
                    scale: isCovering ? 0 : 1,
                  }}
                  transition={{ duration: 0.2 }}
                >
                  {/* Eye shine */}
                  <div className="absolute top-0.5 left-0.5 w-1 h-1 bg-white rounded-full" />
                </motion.div>
              </div>
            </motion.div>

            {/* Right eye */}
            <motion.div
              className="relative"
              animate={{ y: focusedField === 'email' ? 3 : 0 }}
              transition={{ duration: 0.3 }}
            >
              <div className="w-5 h-5 bg-white rounded-full flex items-center justify-center shadow-inner border border-neutral-200">
                <motion.div
                  className="w-2.5 h-2.5 bg-neutral-800 rounded-full relative"
                  animate={{
                    y: focusedField === 'email' ? 1 : 0,
                    // Right eye stays visible when peeking
                    scale: isCovering ? 0 : 1,
                  }}
                  transition={{ duration: 0.2 }}
                >
                  {/* Eye shine */}
                  <div className="absolute top-0.5 left-0.5 w-1 h-1 bg-white rounded-full" />
                </motion.div>
              </div>
            </motion.div>
          </div>

          {/* Nose */}
          <div className="absolute top-14 left-1/2 -translate-x-1/2 w-5 h-4 bg-neutral-800 rounded-[50%] shadow-sm" />

          {/* Mouth */}
          <div className="absolute top-[4.2rem] left-1/2 -translate-x-1/2 flex flex-col items-center">
            <div className="w-0.5 h-2 bg-neutral-700" />
            <motion.div
              className="flex gap-0"
              animate={{ scaleY: isCovering ? 0.5 : 1 }}
            >
              <div className="w-3 h-1.5 border-b-2 border-l-2 border-neutral-700 rounded-bl-full" />
              <div className="w-3 h-1.5 border-b-2 border-r-2 border-neutral-700 rounded-br-full" />
            </motion.div>
          </div>

          {/* Tongue - visible when happy (email focus) */}
          <motion.div
            className="absolute top-[5rem] left-1/2 -translate-x-1/2 w-3 h-3 bg-pink-400 rounded-b-full"
            initial={{ scaleY: 0, originY: 0 }}
            animate={{
              scaleY: focusedField === 'email' ? 1 : 0,
            }}
            transition={{ duration: 0.2 }}
          />
        </div>

        {/* Paws covering eyes */}
        <motion.div
          className="absolute top-6 left-0 right-0 flex justify-center pointer-events-none z-20"
          initial={{ y: 50, opacity: 0 }}
          animate={{
            y: isCovering || isPeeking ? 0 : 50,
            opacity: isCovering || isPeeking ? 1 : 0,
          }}
          transition={{ duration: 0.3, ease: "easeOut" }}
        >
          {/* Left paw - covers left eye, stays when peeking */}
          <motion.div
            className="w-10 h-9 bg-gradient-to-b from-amber-500 to-amber-600 rounded-full shadow-lg relative border-2 border-amber-700"
            animate={{
              x: isPeeking ? -4 : 0,
              rotate: isPeeking ? -5 : 0
            }}
            transition={{ duration: 0.2 }}
          >
            {/* Paw pad */}
            <div className="absolute bottom-1.5 left-1/2 -translate-x-1/2 w-4 h-3 bg-amber-800/40 rounded-full" />
            <div className="absolute bottom-4 left-1/2 -translate-x-1/2 flex gap-0.5">
              <div className="w-1.5 h-1.5 bg-amber-800/40 rounded-full" />
              <div className="w-1.5 h-1.5 bg-amber-800/40 rounded-full" />
              <div className="w-1.5 h-1.5 bg-amber-800/40 rounded-full" />
            </div>
          </motion.div>

          {/* Right paw - disappears when peeking */}
          <motion.div
            className="w-10 h-9 bg-gradient-to-b from-amber-500 to-amber-600 rounded-full shadow-lg relative border-2 border-amber-700 -ml-1"
            animate={{
              opacity: isPeeking ? 0 : 1,
              x: isPeeking ? 20 : 0,
              scale: isPeeking ? 0.5 : 1
            }}
            transition={{ duration: 0.25 }}
          >
            {/* Paw pad */}
            <div className="absolute bottom-1.5 left-1/2 -translate-x-1/2 w-4 h-3 bg-amber-800/40 rounded-full" />
            <div className="absolute bottom-4 left-1/2 -translate-x-1/2 flex gap-0.5">
              <div className="w-1.5 h-1.5 bg-amber-800/40 rounded-full" />
              <div className="w-1.5 h-1.5 bg-amber-800/40 rounded-full" />
              <div className="w-1.5 h-1.5 bg-amber-800/40 rounded-full" />
            </div>
          </motion.div>
        </motion.div>
      </div>
    </div>
  )
}
