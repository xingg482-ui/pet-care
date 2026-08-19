const ALLOWED_AVATAR_TYPES = ['image/jpeg', 'image/png', 'image/webp']
const MAX_AVATAR_SIZE = 2 * 1024 * 1024

export function validateAvatarFile(file) {
  if (!file) {
    return false
  }
  if (!ALLOWED_AVATAR_TYPES.includes(file.type)) {
    throw new Error('头像仅支持 jpg、png、webp 格式')
  }
  if (file.size > MAX_AVATAR_SIZE) {
    throw new Error('头像文件不能超过2MB')
  }
  return true
}

export function canvasToAvatarFile(canvas, sourceName) {
  return new Promise((resolve, reject) => {
    canvas.toBlob((blob) => {
      if (!blob) {
        reject(new Error('头像裁剪失败，请重新选择图片'))
        return
      }
      const baseName = sourceName.replace(/\.[^.]+$/, '') || 'avatar'
      resolve(new File([blob], `${baseName}-avatar.png`, { type: 'image/png' }))
    }, 'image/png', 0.92)
  })
}
