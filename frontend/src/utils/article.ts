/**
 * 文章相关工具函数
 */
import { STATUS_TEXT_MAP, STATUS_TAG_COLOR_MAP, STATUS_COLOR_MAP } from '@/constants/article'

/**
 * 获取状态文本
 * @param status 状态值
 */
export const getStatusText = (status: string): string => {
  return STATUS_TEXT_MAP[status] || status
}

/**
 * 获取状态标签颜色（用于 Ant Design Tag）
 * @param status 状态值
 */
export const getStatusTagColor = (status: string): string => {
  return STATUS_TAG_COLOR_MAP[status] || 'default'
}

/**
 * 获取状态颜色（用于自定义样式）
 * @param status 状态值
 */
export const getStatusColor = (status: string): string => {
  return STATUS_COLOR_MAP[status] || '#999'
}

export interface ArticleImageMarkdown {
  url?: string
  description?: string
  sectionTitle?: string
  position?: number
  placeholderId?: string
}

const escapeRegExp = (value: string): string => value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

/** 将配图补回正文，兼容占位符、章节标题和旧数据。 */
export const mergeArticleImages = (markdown: string, images?: ArticleImageMarkdown[]): string => {
  let result = markdown || ''
  if (!images?.length) return result

  const placeholderCandidates = (placeholderId?: string) => {
    if (!placeholderId?.trim()) return []
    const token = placeholderId.trim().replace(/^\{+|\}+$/g, '')
    return [placeholderId.trim(), `{{${token}}}`, `{{{{${token}}}}}`]
  }

  images
    .filter((image) => image.position !== 1 && image.url)
    .forEach((image) => {
      const url = image.url as string
      if (result.includes(`](${url})`)) return

      const imageMarkdown = `![${image.description || image.sectionTitle || '文章配图'}](${url})`
      const candidates = placeholderCandidates(image.placeholderId)
      if (candidates.length) {
        const matchedPlaceholder = candidates.find((candidate) => result.includes(candidate))
        if (matchedPlaceholder) {
          result = result.split(matchedPlaceholder).join(imageMarkdown)
          return
        }
      }

      const sectionTitle = image.sectionTitle?.trim()
      if (sectionTitle) {
        const headingPattern = new RegExp(
          `(^|\\n)(#{1,6})\\s*${escapeRegExp(sectionTitle)}\\s*(?=\\n|$)`,
          'i',
        )
        const headingMatch = headingPattern.exec(result)
        if (headingMatch && headingMatch.index !== undefined) {
          const insertAt = headingMatch.index + headingMatch[0].length
          result = `${result.slice(0, insertAt)}\n\n${imageMarkdown}${result.slice(insertAt)}`
          return
        }
      }

      result = image.position === 1
        ? `${imageMarkdown}\n\n${result}`
        : `${result.trimEnd()}\n\n${imageMarkdown}`
    })

  return result
}

/**
 * 导出文章为 Markdown 文件
 * @param title 文章标题
 * @param subTitle 副标题
 * @param content 正文内容
 * @param fullContent 完整图文内容（可选）
 * @param outline 大纲（可选）
 * @param images 配图列表（可选）
 */
export interface ExportArticleOptions {
  title: string
  subTitle?: string
  content?: string
  fullContent?: string
  outline?: Array<{ section: number; title: string }>
  images?: ArticleImageMarkdown[]
}

export const exportAsMarkdown = (options: ExportArticleOptions): void => {
  const { title, subTitle, content, fullContent, outline, images } = options

  let markdown = `# ${title}\n\n`
  if (subTitle) {
    markdown += `> ${subTitle}\n\n`
  }

  // 优先使用完整图文
  if (fullContent) {
    markdown += mergeArticleImages(fullContent, images)
  } else {
    if (outline && outline.length > 0) {
      markdown += `## 目录\n\n`
      outline.forEach((item) => {
        markdown += `${item.section}. ${item.title}\n`
      })
      markdown += `\n---\n\n`
    }

    markdown += content || ''

    const bodyImages = images?.filter((image) => image.position !== 1 && image.url)
    if (bodyImages && bodyImages.length > 0) {
      markdown += `\n\n## 配图\n\n`
      bodyImages.forEach((image) => {
        markdown += `![${image.description || image.sectionTitle || '文章配图'}](${image.url})\n\n`
      })
    }
  }

  const blob = new Blob([markdown], { type: 'text/markdown' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${title || '文章'}.md`
  a.click()
  URL.revokeObjectURL(url)
}
