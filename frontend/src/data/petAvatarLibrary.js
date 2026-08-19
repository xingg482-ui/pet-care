import americanShorthairAvatar from '../assets/pet-avatars/american-shorthair.png'
import bichonAvatar from '../assets/pet-avatars/bichon.png'
import borderCollieAvatar from '../assets/pet-avatars/border-collie.png'
import britishShorthairAvatar from '../assets/pet-avatars/british-shorthair.png'
import corgiAvatar from '../assets/pet-avatars/corgi.png'
import exoticShorthairAvatar from '../assets/pet-avatars/exotic-shorthair.png'
import goldenRetrieverAvatar from '../assets/pet-avatars/golden-retriever.png'
import goldenShadedAvatar from '../assets/pet-avatars/golden-shaded.png'
import huskyAvatar from '../assets/pet-avatars/husky.png'
import labradorAvatar from '../assets/pet-avatars/labrador.png'
import liHuaAvatar from '../assets/pet-avatars/li-hua.png'
import maineCoonAvatar from '../assets/pet-avatars/maine-coon.png'
import orangeTabbyAvatar from '../assets/pet-avatars/orange-tabby.png'
import pomeranianAvatar from '../assets/pet-avatars/pomeranian.png'
import poodleAvatar from '../assets/pet-avatars/poodle.png'
import ragdollAvatar from '../assets/pet-avatars/ragdoll.png'
import samoyedAvatar from '../assets/pet-avatars/samoyed.png'
import shibaAvatar from '../assets/pet-avatars/shiba.png'
import siameseAvatar from '../assets/pet-avatars/siamese.png'
import silverShadedAvatar from '../assets/pet-avatars/silver-shaded.png'

export const petAvatarLibrary = [
  {
    species: '狗',
    breed: '柯基',
    keywords: ['柯基', '威尔士柯基', 'corgi'],
    src: corgiAvatar,
  },
  {
    species: '狗',
    breed: '金毛',
    keywords: ['金毛', '金毛寻回犬', 'golden retriever', 'golden-retriever'],
    src: goldenRetrieverAvatar,
  },
  {
    species: '狗',
    breed: '拉布拉多',
    keywords: ['拉布拉多', 'labrador'],
    src: labradorAvatar,
  },
  {
    species: '狗',
    breed: '贵宾',
    keywords: ['贵宾', '泰迪', 'poodle', 'teddy'],
    src: poodleAvatar,
  },
  {
    species: '狗',
    breed: '博美',
    keywords: ['博美', 'pomeranian'],
    src: pomeranianAvatar,
  },
  {
    species: '狗',
    breed: '比熊',
    keywords: ['比熊', 'bichon'],
    src: bichonAvatar,
  },
  {
    species: '狗',
    breed: '哈士奇',
    keywords: ['哈士奇', 'husky', 'siberian husky'],
    src: huskyAvatar,
  },
  {
    species: '狗',
    breed: '萨摩耶',
    keywords: ['萨摩耶', 'samoyed'],
    src: samoyedAvatar,
  },
  {
    species: '狗',
    breed: '柴犬',
    keywords: ['柴犬', 'shiba', 'shiba inu'],
    src: shibaAvatar,
  },
  {
    species: '狗',
    breed: '边牧',
    keywords: ['边牧', '边境牧羊犬', 'border collie', 'border-collie'],
    src: borderCollieAvatar,
  },
  {
    species: '猫',
    breed: '英短',
    keywords: ['英短', '英国短毛', 'british shorthair', 'british-shorthair'],
    src: britishShorthairAvatar,
  },
  {
    species: '猫',
    breed: '美短',
    keywords: ['美短', '美国短毛', 'american shorthair', 'american-shorthair'],
    src: americanShorthairAvatar,
  },
  {
    species: '猫',
    breed: '布偶',
    keywords: ['布偶', 'ragdoll'],
    src: ragdollAvatar,
  },
  {
    species: '猫',
    breed: '暹罗',
    keywords: ['暹罗', 'siamese'],
    src: siameseAvatar,
  },
  {
    species: '猫',
    breed: '缅因',
    keywords: ['缅因', 'maine coon', 'maine-coon'],
    src: maineCoonAvatar,
  },
  {
    species: '猫',
    breed: '加菲',
    keywords: ['加菲', '异国短毛', 'exotic shorthair', 'exotic-shorthair'],
    src: exoticShorthairAvatar,
  },
  {
    species: '猫',
    breed: '橘猫',
    keywords: ['橘猫', 'orange tabby', 'orange-tabby'],
    src: orangeTabbyAvatar,
  },
  {
    species: '猫',
    breed: '狸花',
    keywords: ['狸花', '中华田园猫', 'li hua', 'li-hua', 'tabby'],
    src: liHuaAvatar,
  },
  {
    species: '猫',
    breed: '银渐层',
    keywords: ['银渐层', 'silver shaded', 'silver-shaded'],
    src: silverShadedAvatar,
  },
  {
    species: '猫',
    breed: '金渐层',
    keywords: ['金渐层', 'golden shaded', 'golden-shaded'],
    src: goldenShadedAvatar,
  },
]

export function resolvePetAvatar(pet) {
  const breed = `${pet?.breed || ''}`.toLowerCase()
  const species = `${pet?.species || ''}`.toLowerCase()
  const petText = `${breed} ${species}`

  return petAvatarLibrary.find((item) => (
    item.keywords.some((keyword) => petText.includes(keyword.toLowerCase()))
  ))?.src || ''
}
