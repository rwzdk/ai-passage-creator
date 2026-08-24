<template>
  <div ref="createPageRef" class="article-create-page">
    <!-- 三栏布局容器 -->
    <div class="create-layout">
      <!-- 左侧：智能体流程可视化 -->
      <aside :class="['sidebar-left', { 'has-article-summary': currentPhase === 'COMPLETED' }]">
        <div class="sidebar-header">
          <h3 class="sidebar-title">创作流程</h3>
          <p class="sidebar-subtitle">智能体协作可视化</p>
        </div>

        <div class="flow-timeline">
          <div
            v-for="(step, index) in agentSteps"
            :key="index"
            :class="[
              'flow-item',
              {
                active: currentStep === index,
                completed: currentStep > index,
                pending: currentStep < index,
                failed: currentStep === index && currentStepStatus === 'failed',
              },
            ]"
          >
            <div class="flow-indicator">
              <LoadingOutlined
                v-if="currentStep === index && isCreating && currentStepStatus !== 'failed'"
                class="spin-icon"
              />
              <CheckCircleOutlined v-else-if="currentStep > index" />
              <span v-else class="step-number">{{ index + 1 }}</span>
            </div>
            <div class="flow-content">
              <div class="flow-title">{{ step.title }}</div>
              <div class="flow-desc">{{ step.description }}</div>
              <div v-if="currentStep === index && isCreating" class="flow-status">
                <span class="status-dot" :class="{ failed: currentStepStatus === 'failed' }"></span>
                {{ currentStepStatusText || '执行中...' }}
              </div>
              <div v-else-if="currentStep > index" class="flow-status completed-status">
                已完成
              </div>
            </div>
          </div>
        </div>

        <section v-if="currentPhase === 'COMPLETED'" class="article-summary-panel">
          <div class="article-summary-heading">
            <div>
              <span class="article-summary-kicker">快速了解</span>
              <h4>文章总结</h4>
            </div>
            <FileTextOutlined />
          </div>
          <div v-if="articleSummary.fullSummary" class="article-summary-overview">
            <span class="article-summary-label">全文总结</span>
            <p class="article-summary-intro">{{ articleSummary.fullSummary.intro }}</p>
            <div v-if="articleSummary.fullSummary.points.length" class="article-summary-points">
              <div
                v-for="(point, index) in articleSummary.fullSummary.points"
                :key="point.title"
                class="article-summary-point"
              >
                <span class="article-summary-point-index">{{ String(index + 1).padStart(2, '0') }}</span>
                <div class="article-summary-point-content">
                  <strong>{{ point.title }}</strong>
                  <span v-if="point.detail">{{ point.detail }}</span>
                </div>
              </div>
            </div>
          </div>
        </section>
      </aside>

      <!-- 中间：主内容区 -->
      <main ref="mainContentRef" class="main-content">
          <!-- 阶段切换（带轻量过渡动画） -->
          <Transition name="stage" mode="out-in">
          <!-- 输入状态 -->
          <div v-if="currentPhase === 'INPUT'" key="input" class="input-state">
            <div class="input-card">
              <div class="input-header">
                <div class="creative-pulse" aria-label="AI 创作引擎已就绪">
                  <ThunderboltOutlined />
                  <span></span><span></span><span></span>
                </div>
                <h1 class="input-title">创作新文章</h1>
                <p class="input-subtitle">输入选题，AI 帮你生成爆款文章</p>
              </div>

              <div class="input-area">
                <a-textarea
                  v-model:value="topic"
                  placeholder="请输入您想创作的文章选题，例如：2026年AI如何改变职场"
                  :rows="4"
                  :maxlength="500"
                  show-count
                  class="topic-textarea"
                />

                <div class="reference-upload">
                  <div class="reference-upload-header">
                    <div>
                      <div class="section-title">参考文档</div>
                      <div class="section-tip">
                        支持 PDF、Word、TXT、Markdown，单个文件最大 20 MB
                      </div>
                    </div>
                    <a-button
                      type="default"
                      :loading="isReferenceParsing"
                      :disabled="isReferenceParsing"
                      @click="openReferencePicker"
                    >
                      <template #icon><UploadOutlined /></template>
                      {{ referenceItems.length ? '继续添加' : '上传文件' }}
                    </a-button>
                    <input
                      ref="referenceFileInput"
                      class="reference-file-input"
                      type="file"
                      accept=".pdf,.doc,.docx,.txt,.md"
                      multiple
                      @change="handleReferenceFileChange"
                    />
                  </div>
                  <div v-if="isReferenceParsing" class="reference-status is-loading">
                    <a-spin size="small" />
                    <span>正在提取并总结参考材料...</span>
                  </div>
                  <div v-if="referenceItems.length" class="reference-list">
                    <div v-for="item in referenceItems" :key="item.id" class="reference-summary">
                      <div class="reference-summary-meta">
                        <span class="reference-file-name"
                          ><FileTextOutlined /> {{ item.fileName }}</span
                        >
                        <span>{{ item.characterCount.toLocaleString() }} 字符已纳入参考</span>
                        <span class="reference-actions">
                          <a-button
                            type="text"
                            size="small"
                            :aria-label="item.expanded ? '收起参考内容' : '展开参考内容'"
                            @click="item.expanded = !item.expanded"
                          >
                            <template #icon
                              ><CaretDownOutlined v-if="item.expanded" /><CaretRightOutlined v-else
                            /></template>
                          </a-button>
                          <a-button
                            type="text"
                            danger
                            size="small"
                            aria-label="移除参考文档"
                            @click="clearReference(item.id)"
                          >
                            <template #icon><DeleteOutlined /></template>
                          </a-button>
                        </span>
                      </div>
                      <p v-if="item.expanded">{{ item.summary }}</p>
                    </div>
                  </div>
                  <div v-else-if="!isReferenceParsing" class="reference-empty">
                    <FileTextOutlined />
                    <span>未上传参考文档</span>
                  </div>
                  <div v-if="referenceError" class="reference-error">{{ referenceError }}</div>
                </div>

                <!-- 文章风格选择 -->
                <div class="style-section">
                  <div class="section-header">
                    <span class="section-title">文章风格</span>
                    <span class="section-tip">（不选择使用默认风格）</span>
                  </div>
                  <a-radio-group v-model:value="selectedStyle" class="style-group">
                    <a-radio value="">默认</a-radio>
                    <a-radio value="tech">科技风格</a-radio>
                    <a-radio value="emotional">情感风格</a-radio>
                    <a-radio value="educational">教育风格</a-radio>
                    <a-radio value="humorous">轻松幽默</a-radio>
                  </a-radio-group>
                </div>

                <!-- 配图方式选择 -->
                <div class="image-methods-section">
                  <div class="section-header">
                    <span class="section-title">配图方式</span>
                    <span class="section-tip">（不选择表示支持所有方式）</span>
                  </div>
                  <a-checkbox-group v-model:value="selectedImageMethods" class="methods-group">
                    <a-checkbox value="PEXELS">Pexels</a-checkbox>
                    <a-tooltip :title="isVip ? '' : '仅限 VIP 会员'">
                      <a-checkbox value="NANO_BANANA" :disabled="!isVip">
                        Nano Banana
                        <CrownOutlined v-if="!isVip" class="vip-icon" />
                      </a-checkbox>
                    </a-tooltip>
                    <a-tooltip :title="isVip ? '' : '仅限 VIP 会员'">
                      <a-checkbox value="IMAGE_2" :disabled="!isVip">
                        GPT Image 2
                        <CrownOutlined v-if="!isVip" class="vip-icon" />
                      </a-checkbox>
                    </a-tooltip>
                    <a-checkbox value="MERMAID">Mermaid</a-checkbox>
                    <a-checkbox value="ICONIFY">Iconify</a-checkbox>
                    <a-checkbox value="EMOJI_PACK">表情包</a-checkbox>
                    <a-tooltip :title="isVip ? '' : '仅限 VIP 会员'">
                      <a-checkbox value="SVG_DIAGRAM" :disabled="!isVip">
                        SVG
                        <CrownOutlined v-if="!isVip" class="vip-icon" />
                      </a-checkbox>
                    </a-tooltip>
                  </a-checkbox-group>
                  <div v-if="!isVip" class="vip-notice">
                    <CrownOutlined />
                    <span>AI 生图和 SVG 图表为 VIP 专属功能，</span>
                    <RouterLink to="/vip" class="upgrade-link">立即升级</RouterLink>
                  </div>
                </div>

                <a-button
                  type="primary"
                  size="large"
                  :loading="isCreating"
                  :disabled="!topic.trim() || !hasQuota"
                  @click="startCreate"
                  class="create-btn"
                >
                  <template #icon>
                    <RocketOutlined />
                  </template>
                  开始创作
                </a-button>
                <div v-if="!hasQuota" class="quota-warning">
                  <WarningOutlined />
                  <span>配额已用完，无法创建文章</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 标题生成中 -->
          <div
            v-else-if="currentPhase === 'TITLE_GENERATING'"
            key="title-generating"
            class="loading-stage"
          >
            <div class="loading-stage-icon" aria-hidden="true"><LoadingOutlined /></div>
            <div class="loading-stage-copy">
              <span class="loading-eyebrow">AI 正在工作</span>
              <h3>正在生成标题方案</h3>
              <p>先梳理选题，再为你准备多个可选方向</p>
            </div>
            <AnimatedSkeleton :lines="3" />
          </div>

          <!-- 标题选择阶段 -->
          <TitleSelectingStage
            v-else-if="currentPhase === 'TITLE_SELECTING'"
            key="title-selecting"
            :title-options="titleOptions"
            :loading="confirmLoading"
            @confirm="handleConfirmTitle"
          />

          <!-- 大纲生成中（流式展示） -->
          <div
            v-else-if="currentPhase === 'OUTLINE_GENERATING'"
            key="outline-generating"
            class="outline-generating-state"
          >
            <!-- 标题预览 -->
            <div v-if="article.mainTitle" class="preview-header">
              <h1 class="article-title">{{ article.mainTitle }}</h1>
              <p class="article-subtitle">{{ article.subTitle }}</p>
            </div>

            <!-- 大纲流式展示 -->
            <div class="outline-preview">
              <div class="section-label">
                <BulbOutlined />
                <span>AI 正在规划文章大纲</span>
                <span class="typing-cursor">|</span>
              </div>
              <div v-if="parsedOutline.length > 0" class="outline-list">
                <div v-for="item in parsedOutline" :key="item.section" class="outline-item fade-in">
                  <div class="outline-title">{{ item.section }}. {{ item.title }}</div>
                  <ul class="outline-points">
                    <li v-for="(point, idx) in item.points" :key="idx">{{ point }}</li>
                  </ul>
                </div>
              </div>
              <div v-else class="outline-loading">
                <LoadingOutlined class="spin-icon" />
                <span>正在构建文章结构...</span>
                <AnimatedSkeleton :lines="2" />
              </div>
            </div>
          </div>

          <!-- 大纲编辑阶段 -->
          <OutlineEditingStage
            v-else-if="currentPhase === 'OUTLINE_EDITING'"
            key="outline-editing"
            :outline="outline"
            :loading="confirmLoading"
            :task-id="taskId"
            @confirm="handleConfirmOutline"
          />

          <!-- 正文生成阶段 -->
          <div
            v-else-if="currentPhase === 'CONTENT_GENERATING'"
            key="content-generating"
            class="creating-state"
          >
            <!-- 标题预览 -->
            <div v-if="article.mainTitle" class="preview-header">
              <h1 class="article-title">{{ article.mainTitle }}</h1>
              <p class="article-subtitle">{{ article.subTitle }}</p>
            </div>

            <!-- 大纲预览（流式解析展示） -->
            <div v-if="outlineRaw" class="outline-preview">
              <div class="section-label">
                <BulbOutlined />
                <span>文章大纲</span>
                <span v-if="isOutlineStreaming" class="typing-cursor">|</span>
              </div>
              <div class="outline-list">
                <div v-for="item in parsedOutline" :key="item.section" class="outline-item">
                  <div class="outline-title">{{ item.section }}. {{ item.title }}</div>
                  <ul class="outline-points">
                    <li v-for="(point, idx) in item.points" :key="idx">{{ point }}</li>
                  </ul>
                </div>
              </div>
            </div>

            <!-- 正文预览（流式输出） -->
            <div v-if="article.content" class="content-preview">
              <div v-html="streamingMarkdownHtml" class="markdown-body"></div>
              <span v-if="isStreaming" class="typing-cursor">|</span>
            </div>
            <div v-else class="content-loading" aria-live="polite">
              <div class="loading-stage-icon" aria-hidden="true"><LoadingOutlined /></div>
              <div>
                <span class="loading-eyebrow">下一步</span>
                <p>正文即将开始生成，首段内容会实时出现在这里</p>
              </div>
              <AnimatedSkeleton :lines="4" />
            </div>

            <!-- 配图进度 -->
            <div v-if="currentStep === 4 && totalImages > 0" class="image-progress-box">
              <div class="progress-header">
                <PictureOutlined />
                <span>{{ currentStepStatusText || '正在生成配图' }}</span>
              </div>
              <a-progress
                :percent="imageProgress"
                status="active"
                :stroke-color="{ from: '#22C55E', to: '#16A34A' }"
              />
              <p class="progress-hint">
                {{ imageCount }}/{{ totalImages }} 张图片已完成
                <span v-if="imageFailedCount > 0">，{{ imageFailedCount }} 张失败</span>
              </p>
            </div>

            <!-- 加载占位 -->
            <div v-if="currentStep === 0 && !article.mainTitle" class="loading-placeholder">
              <a-spin size="large" />
              <p>AI 正在构思标题...</p>
            </div>
          </div>

          <!-- 创作完成 -->
          <div v-else-if="currentPhase === 'COMPLETED'" key="completed" class="completed-state">
            <div class="success-header">
              <CheckCircleFilled class="success-icon" />
              <span>文章创作完成！</span>
            </div>

            <div class="preview-header">
              <template v-if="isEditingArticle">
                <a-input
                  v-model:value="editableMainTitle"
                  class="article-title-input"
                  placeholder="请输入主标题"
                  :maxlength="200"
                />
                <a-input
                  v-model:value="editableSubTitle"
                  class="article-subtitle-input"
                  placeholder="请输入副标题"
                  :maxlength="300"
                />
              </template>
              <template v-else>
                <h1 class="article-title">{{ article.mainTitle }}</h1>
                <p class="article-subtitle">{{ article.subTitle }}</p>
              </template>
            </div>
            <div v-if="isEditingArticle" class="article-editor">
                <a-textarea
                  v-model:value="editableContent"
                  :rows="24"
                  :maxlength="100000"
                  show-count
                  class="article-editor-textarea"
                />
                <section class="inline-ai-editor content-ai-editor">
                  <div class="inline-ai-editor-title">
                    <BulbOutlined />
                    <span>AI 助手修改正文</span>
                  </div>
                  <div class="inline-ai-editor-body">
                    <a-textarea
                      v-model:value="contentAiInstruction"
                      :rows="3"
                      :maxlength="500"
                      show-count
                      placeholder="告诉 AI 如何修改正文，例如：让开头更有吸引力，并补充一个实际案例"
                    />
                    <a-button
                      type="primary"
                      class="inline-ai-editor-submit"
                      :loading="aiEditing && aiEditMode === 'content'"
                      :disabled="aiEditing || !contentAiInstruction.trim()"
                      @click="runContentAiEdit"
                    >
                      <template #icon><BulbOutlined /></template>
                      AI 修改正文
                    </a-button>
                  </div>
                </section>
                <div v-if="article.images?.length" class="image-editor-section">
                <div class="image-editor-heading">配图</div>
                <div class="image-editor-grid">
                  <div v-for="image in article.images" :key="image.position" class="image-editor-item">
                    <img :src="image.url" :alt="image.description || image.sectionTitle || '文章配图'" />
                    <div class="image-editor-meta">
                      <span>{{ getImageDisplayName(image) }}</span>
                      <a-button size="small" @click="openImageEditor(image)">
                        <template #icon><SwapOutlined /></template>
                        替换图片
                      </a-button>
                    </div>
                  </div>
                  </div>
                </div>
                <section v-if="article.images?.length" class="inline-ai-editor image-ai-editor">
                  <div class="inline-ai-editor-title">
                    <PictureOutlined />
                    <span>AI 助手优化配图</span>
                  </div>
                  <a-select
                    v-model:value="aiEditImagePosition"
                    class="inline-ai-editor-select"
                    placeholder="选择要优化的配图"
                  >
                    <a-select-option v-for="image in article.images" :key="image.position" :value="image.position">
                      {{ getImageDisplayName(image) }}
                    </a-select-option>
                  </a-select>
                  <div class="inline-ai-editor-body">
                    <a-textarea
                      v-model:value="imageAiInstruction"
                      :rows="3"
                      :maxlength="500"
                      show-count
                      placeholder="描述希望的新配图效果，例如：改为明亮的真实摄影风格，突出人物协作场景"
                    />
                    <a-button
                      type="primary"
                      class="inline-ai-editor-submit"
                      :loading="aiEditing && aiEditMode === 'image'"
                      :disabled="aiEditing || !imageAiInstruction.trim() || !aiEditImagePosition"
                      @click="runImageAiEdit"
                    >
                      <template #icon><PictureOutlined /></template>
                      优化并替换配图
                    </a-button>
                  </div>
                </section>
                <div class="article-editor-actions">
                <a-button @click="cancelArticleEdit">取消</a-button>
                <a-button type="primary" :loading="articleSaving" @click="saveArticleEdit">
                  <template #icon><SaveOutlined /></template>
                  保存修改
                </a-button>
              </div>
            </div>
            <div v-else class="content-preview">
              <div class="article-reading-header">
                <div class="article-reading-label">
                  <FileTextOutlined />
                  <span>正文</span>
                </div>
                <span class="article-reading-note">全文阅读</span>
              </div>
              <div
                v-html="markdownToHtml(getArticleBodyContent(article.fullContent || article.content || '', article.images, article.mainTitle))"
                class="markdown-body"
              ></div>
            </div>
          </div>
          </Transition>
      </main>

      <!-- 右侧：辅助面板 -->
      <aside class="sidebar-right">
        <!-- 配额信息 -->
        <div v-if="currentPhase === 'INPUT'" class="panel-section quota-section">
          <h4 class="panel-title">
            <CrownOutlined />
            创作配额
          </h4>
          <div v-if="isAdmin" class="quota-admin">
            <span class="quota-badge admin">管理员</span>
            <span class="quota-text">无限次</span>
          </div>
          <div v-else-if="isVip" class="quota-admin">
            <span class="quota-badge vip">VIP 会员</span>
            <span class="quota-text">无限次</span>
          </div>
          <div v-else class="quota-info">
            <div class="quota-display">
              <span class="quota-number" :class="{ low: quota <= 1, empty: quota === 0 }">{{
                quota
              }}</span>
              <span class="quota-unit">次</span>
            </div>
            <div class="quota-label">剩余可用</div>
            <a-progress
              :percent="(quota / 5) * 100"
              :show-info="false"
              :stroke-color="quota <= 1 ? '#ff4d4f' : '#22C55E'"
              size="small"
              class="quota-progress"
            />
          </div>
        </div>

        <!-- 热门选题 -->
        <div v-if="currentPhase === 'INPUT'" class="panel-section">
          <h4 class="panel-title">
            <BulbOutlined />
            热门选题
          </h4>
          <div class="hot-topics-toolbar">
            <span class="hot-topics-meta"
              >{{ hotTopicsSource === 'gnews' ? '实时热点' : '本地推荐'
              }}<span v-if="hotTopicsUpdatedAt">
                · {{ formatHotTopicsTime(hotTopicsUpdatedAt) }} 更新</span
              ></span
            >
            <a-button
              type="text"
              size="small"
              class="hot-topics-refresh"
              :loading="hotTopicsLoading"
              aria-label="刷新热门选题"
              @click="loadHotTopics(true)"
            >
              <ReloadOutlined /> 刷新
            </a-button>
          </div>
          <div class="hot-topics-viewport" aria-live="polite">
            <TransitionGroup name="hot-topic-drop" tag="div" class="hot-tags">
              <span
                v-for="item in visibleHotTopics"
                :key="item.title"
                class="hot-tag"
                @click="selectHotTopic(item)"
              >
                {{ item.title }}
              </span>
            </TransitionGroup>
          </div>
          <div class="panel-footer">
            <button type="button" class="help-link" @click="helpVisible = true">
              <QuestionCircleOutlined />
              使用帮助
            </button>
            <button type="button" class="help-link" @click="feedbackVisible = true">
              <MessageOutlined />
              反馈建议
            </button>
          </div>
        </div>

        <!-- 创作技巧 -->
        <div v-if="currentPhase === 'INPUT'" class="panel-section">
          <h4 class="panel-title">
            <StarOutlined />
            爆款技巧
          </h4>
          <div class="tips-list">
            <div class="tip-item">
              <div class="tip-icon">1</div>
              <div class="tip-content">
                <div class="tip-title">先写具体场景</div>
                <div class="tip-desc">用“谁在什么情况下卡住了”替代泛泛的痛点描述。</div>
              </div>
            </div>
            <div class="tip-item">
              <div class="tip-icon">2</div>
              <div class="tip-content">
                <div class="tip-title">标题留一个答案</div>
                <div class="tip-desc">前半句给结果或冲突，正文再解释关键原因与做法。</div>
              </div>
            </div>
            <div class="tip-item">
              <div class="tip-icon">3</div>
              <div class="tip-content">
                <div class="tip-title">用证据替代形容词</div>
                <div class="tip-desc">补充案例、对比或数据，让“值得看”变得可验证。</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 创作进行中的提示（所有创作阶段） -->
        <div
          v-if="
            isCreating || currentPhase === 'TITLE_SELECTING' || currentPhase === 'OUTLINE_EDITING'
          "
          class="panel-section"
        >
          <h4 class="panel-title">
            <ClockCircleOutlined />
            创作进度
          </h4>
          <div class="progress-info">
            <div class="progress-step">
              <span class="step-label">当前步骤</span>
              <span class="step-value">{{ currentStepLabel }}</span>
            </div>
            <div class="progress-step">
              <span class="step-label">已完成</span>
              <span class="step-value">{{ completedStepCount }}/{{ agentSteps.length }}</span>
            </div>
          </div>
          <div v-if="isCreating" class="progress-tip">
            <InfoCircleOutlined />
            <span>{{ currentStepStatusText || 'AI 正在努力创作中，请耐心等待...' }}</span>
          </div>
          <div v-else class="progress-tip waiting">
            <InfoCircleOutlined />
            <span>等待您的确认...</span>
          </div>
        </div>

        <!-- 实时执行日志 -->
        <div v-if="taskId" class="panel-section realtime-logs-section">
          <h4 class="panel-title">
            <FileTextOutlined />
            执行日志
          </h4>
          <div v-if="realtimeLogs.length" class="logs-container">
            <div
              v-for="(log, index) in realtimeLogs"
              :key="index"
              :class="['log-entry', log.level]"
            >
              <span class="log-time">{{ formatLogTime(log.timestamp) }}</span>
              <span class="log-message">{{ log.message }}</span>
            </div>
          </div>
          <div v-else class="logs-empty">正在同步执行记录...</div>
        </div>

        <!-- 当前选题提示 -->
        <div
          v-if="currentPhase !== 'INPUT' && currentPhase !== 'COMPLETED' && topic"
          class="panel-section"
        >
          <h4 class="panel-title">
            <BulbOutlined />
            创作选题
          </h4>
          <div class="topic-display">
            <p>{{ topic }}</p>
          </div>
        </div>

        <!-- 阶段提示 -->
        <div v-if="currentPhase === 'TITLE_GENERATING'" class="panel-section tips-section">
          <h4 class="panel-title">
            <StarOutlined />
            提示
          </h4>
          <div class="tips-list">
            <div class="tip-item">
              <div class="tip-icon">💡</div>
              <div class="tip-content">
                <div class="tip-desc">AI 正在分析您的选题，生成多个吸引眼球的标题方案</div>
              </div>
            </div>
          </div>
        </div>

        <div v-if="currentPhase === 'TITLE_SELECTING'" class="panel-section tips-section">
          <h4 class="panel-title">
            <StarOutlined />
            提示
          </h4>
          <div class="tips-list">
            <div class="tip-item">
              <div class="tip-icon">✅</div>
              <div class="tip-content">
                <div class="tip-desc">
                  选择最符合您期望的标题，或添加补充描述让 AI 更好地理解您的需求
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-if="currentPhase === 'OUTLINE_GENERATING'" class="panel-section tips-section">
          <h4 class="panel-title">
            <StarOutlined />
            提示
          </h4>
          <div class="tips-list">
            <div class="tip-item">
              <div class="tip-icon">📝</div>
              <div class="tip-content">
                <div class="tip-desc">AI 正在为您规划文章结构，构建清晰的章节脉络</div>
              </div>
            </div>
          </div>
        </div>

        <div v-if="currentPhase === 'OUTLINE_EDITING'" class="panel-section tips-section">
          <h4 class="panel-title">
            <StarOutlined />
            编辑技巧
          </h4>
          <div class="tips-list">
            <div class="tip-item">
              <div class="tip-icon">1</div>
              <div class="tip-content">
                <div class="tip-title">拖动排序</div>
                <div class="tip-desc">点击章节左侧拖动图标可调整章节顺序</div>
              </div>
            </div>
            <div class="tip-item">
              <div class="tip-icon">2</div>
              <div class="tip-content">
                <div class="tip-title">AI 助手</div>
                <div class="tip-desc">使用 AI 助手快速修改大纲结构</div>
              </div>
            </div>
            <div class="tip-item">
              <div class="tip-icon">3</div>
              <div class="tip-content">
                <div class="tip-title">添加章节</div>
                <div class="tip-desc">根据需要添加或删除章节和要点</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div v-if="currentPhase === 'COMPLETED'" class="panel-section">
          <h4 class="panel-title">
            <ThunderboltOutlined />
            快捷操作
          </h4>
          <div class="action-list">
            <a-button block @click="copyContent" class="action-btn">
              <CopyOutlined />
              复制全文
            </a-button>
            <a-button block @click="viewArticle" class="action-btn">
              <EyeOutlined />
              查看详情
            </a-button>
            <a-button
              block
              class="action-btn"
              @click="isEditingArticle ? cancelArticleEdit() : startArticleEdit()"
            >
              <EditOutlined />
              {{ isEditingArticle ? '退出编辑' : '编辑文章' }}
            </a-button>
            <a-button block type="primary" @click="exportArticle" class="action-btn primary">
              <DownloadOutlined />
              导出文章
            </a-button>
            <a-button block type="primary" @click="resetCreate" class="action-btn primary">
              <RedoOutlined />
              再创作一章
            </a-button>
          </div>
        </div>

        <!-- 完成后的统计：紧跟快捷操作，避免被侧栏剩余空间推到底部 -->
        <div v-if="currentPhase === 'COMPLETED'" class="panel-section stats-section">
          <h4 class="panel-title">
            <BarChartOutlined />
            文章统计
          </h4>
          <div class="stats-grid">
            <div class="stat-item">
              <div class="stat-value">
                {{ (article.fullContent || article.content || '').length }}
              </div>
              <div class="stat-label">字数</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ article.images?.length || 0 }}</div>
              <div class="stat-label">配图</div>
            </div>
          </div>
        </div>

        <div
          v-if="currentPhase === 'COMPLETED' && isEditingArticle"
          class="panel-section realtime-logs-section edit-activity-log-section"
        >
          <h4 class="panel-title">
            <ClockCircleOutlined />
            修改进度
            <span class="edit-activity-log-hint">本次编辑记录</span>
          </h4>
          <div class="logs-container" aria-live="polite">
            <div
              v-for="log in editActivityLogs"
              :key="log.id"
              :class="['log-entry', log.status]"
            >
              <span class="log-time">
                <a-spin v-if="log.status === 'processing'" size="small" />
                <CheckCircleFilled v-else-if="log.status === 'success'" />
                <WarningOutlined v-else-if="log.status === 'error'" />
                <ClockCircleOutlined v-else />
                {{ formatLogTime(log.timestamp) }}
              </span>
              <span class="log-message">{{ log.message }}</span>
            </div>
          </div>
        </div>

        <div v-if="false" class="panel-section ai-editor-section">
          <h4 class="panel-title">
            <BulbOutlined />
            AI 编辑助手
          </h4>
          <a-radio-group v-model:value="aiEditMode" class="ai-edit-mode" button-style="solid">
            <a-radio-button value="content">文章内容</a-radio-button>
            <a-radio-button value="image">配图优化</a-radio-button>
          </a-radio-group>
          <a-select
            v-if="aiEditMode === 'image'"
            v-model:value="aiEditImagePosition"
            class="ai-edit-image-select"
            placeholder="选择要优化的配图"
          >
            <a-select-option v-for="image in article.images || []" :key="image.position" :value="image.position">
              {{ getImageDisplayName(image) }}
            </a-select-option>
          </a-select>
          <a-textarea
            v-model:value="aiEditInstruction"
            :rows="4"
            :maxlength="500"
            show-count
            :placeholder="
              aiEditMode === 'content'
                ? '例如：把开头改得更有吸引力，并补充一个实际案例'
                : '例如：改成清晨山间的真实摄影风格，画面更明亮'
            "
          />
          <a-button
            type="primary"
            block
            :loading="aiEditing"
            :disabled="aiEditMode === 'image' && !aiEditImagePosition"
            @click="runAiEdit"
          >
            <template #icon><ThunderboltOutlined /></template>
            {{ aiEditMode === 'content' ? '生成内容修改' : '优化并替换配图' }}
          </a-button>
          <div class="ai-editor-hint">
            {{ aiEditMode === 'content' ? '生成结果会先放入编辑器，确认后再保存。' : '配图会直接替换文章中的对应图片。' }}
          </div>
        </div>
      </aside>
    </div>

    <!-- 错误提示 -->
    <a-modal v-model:open="errorVisible" title="创作失败" @ok="errorVisible = false">
      <p>{{ errorMessage }}</p>
    </a-modal>

    <a-modal v-model:open="helpVisible" title="使用帮助" :footer="null" :width="640">
      <ol class="help-steps">
        <li><strong>输入选题：</strong>写清目标读者、使用场景和希望解决的问题。</li>
        <li><strong>选择风格与配图：</strong>按文章定位选择写作风格和需要的图片方式。</li>
        <li><strong>确认标题：</strong>从 AI 生成的标题方案中选择最符合主题的一项。</li>
        <li><strong>调整大纲：</strong>补充、删减或修改要点后，再开始生成正文。</li>
        <li><strong>查看成品：</strong>创作完成后可复制全文、查看详情或导出 Markdown。</li>
      </ol>
    </a-modal>

    <a-modal
      v-model:open="feedbackVisible"
      title="反馈建议"
      ok-text="提交反馈"
      cancel-text="取消"
      :width="620"
      :confirm-loading="feedbackSubmitting"
      @ok="handleFeedbackSubmit"
    >
      <a-form layout="vertical">
        <a-form-item label="你的建议" required>
          <a-textarea
            v-model:value="feedbackContent"
            :maxlength="1000"
            :auto-size="{ minRows: 5, maxRows: 10 }"
            show-count
            placeholder="描述你遇到的问题，或告诉我们希望改进的地方"
          />
        </a-form-item>
        <a-form-item label="图片附件（可选）" extra="支持 JPG、JPEG、PNG、WEBP，最多 3 张，单张不超过 5MB">
          <label class="feedback-upload-trigger">
            <UploadOutlined /> 添加图片
            <input type="file" accept="image/jpeg,image/png,image/webp" multiple @change="handleFeedbackImages" />
          </label>
          <div v-if="feedbackImages.length" class="feedback-image-list">
            <div v-for="image in feedbackImages" :key="`${image.name}-${image.lastModified}`" class="feedback-image-item">
              <PictureOutlined />
              <span>{{ image.name }}</span>
              <a-button type="text" size="small" aria-label="移除图片" @click="removeFeedbackImage(image)">
                <DeleteOutlined />
              </a-button>
            </div>
          </div>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>

  <a-modal
    v-model:open="imageEditVisible"
    title="替换图片"
    :footer="null"
  >
    <div v-if="editingImage" class="image-version-picker">
      <div class="image-version-picker-title">选择已保存的图片</div>
      <div class="image-version-picker-hint">点击图片即可替换当前配图</div>
      <div class="image-version-grid">
        <button
          v-for="(version, index) in getImageVersions(editingImage)"
          :key="version.id || version.url"
          type="button"
          :disabled="imageSwitching"
          class="image-version-option"
          :class="{ selected: version.id === editingImage.selectedVersionId }"
          @click="selectImageVersion(version.id)"
        >
          <img :src="version.url" :alt="`图片版本 ${index + 1}`" />
          <span>{{ index === 0 ? '原图' : `优化图 ${index}` }}</span>
          <CheckCircleFilled v-if="version.id === editingImage.selectedVersionId" class="image-version-check" />
        </button>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, onBeforeUnmount, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { gsap } from 'gsap'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import {
  RocketOutlined,
  LoadingOutlined,
  CheckCircleOutlined,
  CheckCircleFilled,
  CopyOutlined,
  EditOutlined,
  EyeOutlined,
  RedoOutlined,
  ThunderboltOutlined,
  BulbOutlined,
  StarOutlined,
  ClockCircleOutlined,
  InfoCircleOutlined,
  BarChartOutlined,
  QuestionCircleOutlined,
  MessageOutlined,
  PictureOutlined,
  WarningOutlined,
  CrownOutlined,
  FileTextOutlined,
  ReloadOutlined,
  SwapOutlined,
  UploadOutlined,
  DeleteOutlined,
  CaretDownOutlined,
  CaretRightOutlined,
  DownloadOutlined,
  SaveOutlined,
} from '@ant-design/icons-vue'
import {
  createArticle,
  confirmTitle,
  confirmOutline,
  getArticle,
  getExecutionLogs,
  parseArticleReference,
  regenerateArticleImage,
  selectArticleImageVersion,
  aiEditArticleContent,
  submitFeedback,
  updateArticleContent,
} from '@/api/articleController'
import { getHotTopics } from '@/api/hotTopicController'
import { connectSSE, closeSSE, type SSEMessage } from '@/utils/sse'
import { exportAsMarkdown, mergeArticleImages } from '@/utils/article'
import {
  isAdmin as checkIsAdmin,
  isVip as checkIsVip,
  hasQuota as checkHasQuota,
} from '@/utils/permission'
import { marked } from 'marked'
import TitleSelectingStage from './components/TitleSelectingStage.vue'
import OutlineEditingStage from './components/OutlineEditingStage.vue'
import AnimatedSkeleton from '@/components/motion/AnimatedSkeleton.vue'
import { useGsapMotion } from '@/composables/useGsapMotion'

const router = useRouter()
const route = useRoute()
const loginUserStore = useLoginUserStore()

// 配额相关计算属性
const isAdmin = computed(() => checkIsAdmin(loginUserStore.loginUser))
const isVip = computed(() => checkIsVip(loginUserStore.loginUser))
const quota = computed(() => loginUserStore.loginUser.quota ?? 0)
const hasQuota = computed(() => checkHasQuota(loginUserStore.loginUser))

// 智能体步骤（对应后端 6 个步骤）
const agentSteps = [
  { title: '生成标题', description: 'AI 分析选题，生成吸睛标题' },
  { title: '规划大纲', description: '构建文章结构，理清脉络' },
  { title: '撰写正文', description: '流式生成高质量文章内容' },
  { title: '分析配图', description: '智能分析配图需求和位置' },
  { title: '生成配图', description: '根据需求生成或匹配文章配图' },
  { title: '图文合成', description: '将配图插入正文，完美呈现' },
]

// 示例选题
const exampleTopics = [
  '2026年AI如何改变职场',
  '程序员如何提升竞争力',
  '远程办公的利与弊',
  '如何培养深度思考',
  '新能源汽车趋势',
  '健康饮食指南',
]

const hotTopics = ref<API.HotTopicItem[]>(
  exampleTopics.map((title) => ({ title, source: '推荐选题' })),
)
const hotTopicsLoading = ref(false)
const hotTopicsUpdatedAt = ref('')
const hotTopicsSource = ref('fallback')
const hotTopicOffset = ref(0)
let hotTopicLoopTimer: number | undefined

const visibleHotTopics = computed(() => {
  const topics = hotTopics.value
  const displayCount = Math.min(6, topics.length)
  if (!displayCount) return []

  return Array.from(
    { length: displayCount },
    (_, index) => topics[(hotTopicOffset.value + index) % topics.length],
  )
})

const stopHotTopicLoop = () => {
  if (hotTopicLoopTimer !== undefined) {
    window.clearInterval(hotTopicLoopTimer)
    hotTopicLoopTimer = undefined
  }
}

const startHotTopicLoop = () => {
  stopHotTopicLoop()
  if (hotTopics.value.length <= 6 || window.matchMedia('(prefers-reduced-motion: reduce)').matches)
    return

  hotTopicLoopTimer = window.setInterval(() => {
    const total = hotTopics.value.length
    hotTopicOffset.value = (hotTopicOffset.value - 1 + total) % total
  }, 3600)
}

// 阶段状态
const currentPhase = ref<string>('INPUT') // INPUT, TITLE_SELECTING, OUTLINE_EDITING, CONTENT_GENERATING, COMPLETED

// 状态
const topic = ref('')
const referenceFileInput = ref<HTMLInputElement | null>(null)
interface ReferenceItem {
  id: number
  fileName: string
  summary: string
  characterCount: number
  expanded: boolean
}
const referenceItems = ref<ReferenceItem[]>([])
const isReferenceParsing = ref(false)
const referenceError = ref('')
const selectedStyle = ref('') // 选中的文章风格（空字符串表示默认）
const selectedImageMethods = ref<string[]>([]) // 选中的配图方式（空数组表示全部）
const isCreating = ref(false)
const isCompleted = ref(false)
const isStreaming = ref(false)
const isOutlineStreaming = ref(false)
const currentStep = ref(0)
const taskId = ref('')
const errorVisible = ref(false)
const errorMessage = ref('')
const helpVisible = ref(false)
const feedbackVisible = ref(false)
const feedbackContent = ref('')
const feedbackImages = ref<File[]>([])
const feedbackSubmitting = ref(false)
const confirmLoading = ref(false)

// 实时日志
interface RealtimeLog {
  timestamp: number
  level: string
  message: string
  source?: 'history' | 'realtime'
}
const realtimeLogs = ref<RealtimeLog[]>([])
const executionLogRecords = ref<API.AgentLog[]>([])

interface EditActivityLog {
  id: number
  timestamp: number
  message: string
  status: 'info' | 'processing' | 'success' | 'error'
}
const editActivityLogs = ref<EditActivityLog[]>([])

// 标题方案
const titleOptions = ref<Array<{ mainTitle: string; subTitle: string }>>([])

// 大纲数据
const outline = ref<Array<{ section: number; title: string; points: string[] }>>([])

// 大纲数据（流式）
const outlineRaw = ref('')
const outlinePreviewRaw = ref('')
let outlinePreviewTimer: number | undefined

const flushOutlinePreview = () => {
  if (outlinePreviewTimer !== undefined) {
    window.clearTimeout(outlinePreviewTimer)
    outlinePreviewTimer = undefined
  }
  outlinePreviewRaw.value = outlineRaw.value
}

const scheduleOutlinePreview = () => {
  if (outlinePreviewTimer !== undefined) return
  outlinePreviewTimer = window.setTimeout(() => {
    outlinePreviewTimer = undefined
    outlinePreviewRaw.value = outlineRaw.value
  }, 100)
}

// 大纲项类型
interface OutlineItem {
  title: string
  points: string[]
  section: number
}

// 解析大纲 JSON（格式为 { "sections": [...] }）
const parsedOutline = computed<OutlineItem[]>(() => {
  if (!outlinePreviewRaw.value) return []

  const str = outlinePreviewRaw.value.trim()

  // 尝试解析完整的 JSON
  try {
    const parsed = JSON.parse(str)
    if (parsed && Array.isArray(parsed.sections)) {
      return parsed.sections
    }
    return []
  } catch {
    // JSON 不完整时，尝试解析已完成的部分
    try {
      // 找到最后一个完整的 section 对象 }
      // 格式: { "sections": [ {...}, {...} ] }
      const sectionsMatch = str.match(/"sections"\s*:\s*\[/)
      if (!sectionsMatch) return []

      const sectionsStart = str.indexOf('[', sectionsMatch.index)
      if (sectionsStart === -1) return []

      // 从 sections 数组开始，找到最后一个完整的 }
      const afterStart = str.substring(sectionsStart)
      const lastBrace = afterStart.lastIndexOf('}')

      if (lastBrace > 0) {
        const partialArray = afterStart.substring(0, lastBrace + 1) + ']'
        const parsed = JSON.parse(partialArray)
        if (Array.isArray(parsed)) {
          return parsed
        }
      }
      return []
    } catch {
      return []
    }
  }
})

// 内容区域引用（用于自动滚动）
const mainContentRef = ref<HTMLElement | null>(null)
const createPageRef = ref<HTMLElement | null>(null)

let stageTimeline: gsap.core.Timeline | null = null
let reducedMotionPreference = false

const animateCurrentStage = () => {
  const stage = mainContentRef.value?.querySelector<HTMLElement>(
    '.input-state, .loading-stage, .outline-generating-state, .creating-state, .completed-state',
  )
  if (!stage) return

  stageTimeline?.kill()
  if (reducedMotionPreference) {
    gsap.set(stage, { clearProps: 'all' })
    return
  }

  stageTimeline = gsap.timeline({ defaults: { ease: 'power3.out' } })
  stageTimeline.fromTo(
    stage,
    { autoAlpha: 0, y: 54, scale: 0.965, clipPath: 'inset(0 0 14% 0)' },
    { autoAlpha: 1, y: 0, scale: 1, clipPath: 'inset(0 0 0% 0)', duration: 1.02 },
  )

  const details = stage.querySelectorAll<HTMLElement>(
    '.input-header, .input-area > *, .preview-header, .outline-preview, .content-preview, .success-header',
  )
  stageTimeline.fromTo(
    details,
    { autoAlpha: 0, y: 22 },
    { autoAlpha: 1, y: 0, duration: 0.72, stagger: 0.09 },
    '-=0.58',
  )
}

useGsapMotion(createPageRef, (element, reducedMotion) => {
  reducedMotionPreference = reducedMotion
  const columns = Array.from(
    element.querySelectorAll<HTMLElement>('.sidebar-left, .main-content, .sidebar-right'),
  )
  const flowItems = Array.from(element.querySelectorAll<HTMLElement>('.flow-item'))
  const panelSections = Array.from(element.querySelectorAll<HTMLElement>('.panel-section'))

  if (reducedMotion) {
    gsap.set([...columns, ...flowItems, ...panelSections], { clearProps: 'all' })
    return
  }

  gsap.fromTo(
    columns,
    { autoAlpha: 0, x: (index: number) => (index === 0 ? -42 : index === 2 ? 42 : 0), y: 18 },
    { autoAlpha: 1, x: 0, y: 0, duration: 1.08, stagger: 0.14, ease: 'power3.out' },
  )
  gsap.fromTo(
    flowItems,
    { autoAlpha: 0, x: -22, y: 18 },
    { autoAlpha: 1, x: 0, y: 0, duration: 0.76, stagger: 0.1, delay: 0.42, ease: 'power3.out' },
  )
  gsap.fromTo(
    panelSections,
    { autoAlpha: 0, x: 24, clipPath: 'inset(0 0 0 12%)' },
    {
      autoAlpha: 1,
      x: 0,
      clipPath: 'inset(0 0 0 0%)',
      duration: 0.82,
      stagger: 0.1,
      delay: 0.35,
      ease: 'power3.out',
    },
  )
})

// 配图进度
const imageCount = ref(0)
const totalImages = ref(5)
const imageProgress = ref(0)
const imageFailedCount = ref(0)
const imageGenerationStarted = ref(false)
const completedImagePositions = ref(new Set<number>())
const currentStepStatus = ref<'working' | 'failed'>('working')
const currentStepStatusText = ref('')
const completedStepCount = computed(() => Math.min(currentStep.value, agentSteps.length))
const currentStepLabel = computed(() => {
  const title = agentSteps[currentStep.value]?.title || '创作完成'
  if (currentPhase.value === 'COMPLETED') return currentStepStatusText.value || title
  return currentStepStatusText.value ? `${title} · ${currentStepStatusText.value}` : title
})

const getImageMethodLabel = (method: unknown) => {
  const value = String(method || '')
  if (value === 'IMAGE_2') return 'GPT Image 2'
  if (value === 'NANO_BANANA' || value.startsWith('NANO_BANANA_')) return 'NANO_BANANA'
  return value
}

const getImageEventDetails = (image: Record<string, unknown> | undefined) => {
  const position = Number(image?.position || image?.['position']) || imageCount.value + imageFailedCount.value + 1
  const method = String(image?.method || image?.imageSource || '')
  const methodLabel = getImageMethodLabel(method)
  return { position, methodLabel }
}

// 文章数据
const article = ref<Partial<API.ArticleVO>>({
  mainTitle: '',
  subTitle: '',
  content: '',
  fullContent: '',
  images: [],
})

const getImageDisplayName = (image: API.ImageItem) => {
  if (image.position === 1) return '文章封面'
  return image.sectionTitle || `配图 ${image.position}`
}

const getImageVersions = (image: API.ImageItem): API.ImageVersion[] => {
  if (image.versions?.length) return image.versions
  return image.url ? [{ url: image.url, prompt: image.keywords }] : []
}

const stripGeneratedArticleHeading = (markdown: string, mainTitle?: string) => {
  let content = markdown || ''

  // 生成服务偶尔会把标题元数据一起写入正文，展示时只保留独立标题区。
  content = content
    .replace(/^\s*主标题\s*[:：][^\r\n]*(?:\r?\n|$)/, '')
    .replace(/^\s*正文\s*[:：]\s*/, '')

  const normalizedTitle = mainTitle?.trim()
  if (normalizedTitle) {
    const escapedTitle = normalizedTitle.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    content = content.replace(
      new RegExp(`^\\s*(?:#\\s*)?${escapedTitle}\\s*(?:\\r?\\n|$)`),
      '',
    )
  }

  return content.trim()
}

const getEditableArticleContent = (articleData: Partial<API.ArticleVO>) => {
  const source = articleData.fullContent || articleData.content || ''
  return stripGeneratedArticleHeading(source, articleData.mainTitle)
    .replace(/!\[[^\]]*\]\([^\)\r\n]*\)\s*/g, '')
}

const isEditingArticle = ref(false)
const editableContent = ref('')
const editableMainTitle = ref('')
const editableSubTitle = ref('')
const articleSaving = ref(false)
const imageEditVisible = ref(false)
const imageSwitching = ref(false)
const editingImage = ref<API.ImageItem | null>(null)
const aiEditMode = ref<'content' | 'image'>('content')
const aiEditInstruction = ref('')
const contentAiInstruction = ref('')
const imageAiInstruction = ref('')
const aiEditImagePosition = ref<number | undefined>(undefined)
const aiEditing = ref(false)

let eventSource: EventSource | null = null
let executionLogsTimer: number | undefined
let pollingFallbackActive = false
let pollingInFlight = false

const stopExecutionLogsPolling = () => {
  if (executionLogsTimer !== undefined) {
    window.clearInterval(executionLogsTimer)
    executionLogsTimer = undefined
  }
}

const getLogLevel = (status?: string) => {
  if (status === 'FAILED') return 'error'
  if (status === 'SUCCESS') return 'success'
  return 'info'
}

const getAgentDisplayName = (agentName: string) => {
  const nameMap: Record<string, string> = {
    agent1_generate_titles: '生成标题',
    agent2_generate_outline: '生成大纲',
    agent3_generate_content: '生成正文',
    agent4_analyze_image_requirements: '分析配图需求',
    agent5_generate_images: '生成配图',
    agent6_merge_content: '图文合成',
    ai_modify_outline: 'AI 修改大纲',
  }
  return nameMap[agentName] || agentName
}

const getHistoryLogMessage = (log: API.AgentLog, imageCompletionSequence?: number) => {
  if (log.agentName?.startsWith('__event_')) {
    try {
      const data = log.outputData ? (JSON.parse(log.outputData) as Record<string, unknown>) : {}
      const type = String(data.type || log.agentName.slice('__event_'.length))
      const image = (data.image || {}) as Record<string, unknown>
      const position = Number(image.position) || 0
      const method = String(image.method || image.imageSource || '')
      const methodLabel = getImageMethodLabel(method)
      const imageSuffix = methodLabel ? ` · ${methodLabel}` : ''
      if (type === 'AGENT1_COMPLETE') return '标题方案生成完成'
      if (type === 'TITLES_GENERATED') return `生成了 ${Array.isArray(data.titleOptions) ? data.titleOptions.length : 0} 个标题方案`
      if (type === 'OUTLINE_GENERATED') return '大纲生成完成，等待确认'
      if (type === 'AGENT3_COMPLETE') return '正文生成完成'
      if (type === 'AGENT4_START') return '开始分析配图需求与插入位置'
      if (type === 'AGENT4_COMPLETE') {
        return `配图需求分析完成，共 ${Array.isArray(data.imageRequirements) ? data.imageRequirements.length : 0} 张`
      }
      if (type === 'IMAGE_START') return '已开启异步任务，正在并行生成配图'
      if (type === 'IMAGE_COMPLETE') {
        const displayPosition = imageCompletionSequence || position || '?'
        return `已生成第 ${displayPosition} 张配图${imageSuffix}`
      }
      if (type === 'IMAGE_FAILED') return `第 ${position || '?'} 张配图生成失败${imageSuffix}`
      if (type === 'IMAGE_SKIPPED') return ''
      if (type === 'AGENT5_COMPLETE') return '所有配图生成完成'
      if (type === 'MERGE_START') {
        const bodyImages = Number(data.bodyImages || 0)
        const coverImages = Number(data.coverImages || 0)
        return `开始图文合成：选取 ${bodyImages} 张正文配图${coverImages > 0 ? `（封面 ${coverImages} 张不参与正文合成）` : ''}`
      }
      if (type === 'MERGE_COMPLETE') return '图文合成完成'
      if (type === 'ALL_COMPLETE') return ''
      if (type === 'ERROR') return `创作失败: ${String(data.message || '未知错误')}`
      // 未映射的内部事件仅用于流程回放，不直接展示原始事件名。
      return ''
    } catch {
      return '执行事件记录解析失败'
    }
  }

  const agentName = log.agentName || '智能体'
  const displayName = getAgentDisplayName(agentName)
  if (!displayName) return ''

  if (log.status === 'FAILED') {
    return `${displayName}失败${log.errorMessage ? `：${log.errorMessage}` : ''}`
  }

  if (log.status === 'RUNNING') {
    return `正在${displayName}`
  }

  const completionMessages: Record<string, string> = {
    agent1_generate_titles: '标题方案生成完成',
    agent2_generate_outline: '大纲生成完成，等待确认',
    agent3_generate_content: '正文生成完成',
    agent4_analyze_image_requirements: '配图需求分析完成',
    agent5_generate_images: '所有配图生成完成',
    agent6_merge_content: '图文合成完成',
    ai_modify_outline: '大纲已通过 AI 修改',
  }
  return completionMessages[agentName] || `${displayName}完成`
}

const toHistoryLog = (log: API.AgentLog, imageCompletionSequence?: number): RealtimeLog => {
  const timestamp = log.startTime ? new Date(log.startTime).getTime() : Date.now()
  return {
    timestamp: Number.isNaN(timestamp) ? Date.now() : timestamp,
    level: getLogLevel(log.status),
    message: getHistoryLogMessage(log, imageCompletionSequence),
    source: 'history',
  }
}

const loadExecutionLogs = async (existingTaskId: string) => {
  try {
    const response = await getExecutionLogs({ taskId: existingTaskId })
    const stats = response.data.data
    const historyLogs = (stats?.logs || [])
      .filter(
        (log) =>
          !log.agentName?.includes('AGENT2_STREAMING') &&
          !log.agentName?.includes('AGENT3_STREAMING') &&
          log.agentName !== '__event_ALL_COMPLETE',
      )
      .sort((a, b) => {
        const aTime = new Date(a.startTime || a.createTime || '').getTime()
        const bTime = new Date(b.startTime || b.createTime || '').getTime()
        return aTime - bTime
      })
    let imageStartShown = false
    let imageCompletionSequence = 0
    executionLogRecords.value = historyLogs
    const realtimeOnlyLogs = realtimeLogs.value.filter((log) => log.source === 'realtime')
    realtimeLogs.value = [...historyLogs
      .filter((log) => {
        if (log.agentName !== '__event_IMAGE_START') return true
        if (imageStartShown) return false
        imageStartShown = true
        return true
      })
      .map((log) => {
        if (log.agentName === '__event_IMAGE_COMPLETE') {
          imageCompletionSequence += 1
          return toHistoryLog(log, imageCompletionSequence)
        }
        return toHistoryLog(log)
      })
      .filter((log) => log.message)
      .map((log) => log as unknown as RealtimeLog), ...realtimeOnlyLogs]
      .sort((a, b) => a.timestamp - b.timestamp)
  } catch (error) {
    console.warn('加载执行日志失败:', error)
  }
}

const isArticleRunning = (status?: string) => status === 'PENDING' || status === 'PROCESSING'

const getExecutionProgress = () => {
  const logs = executionLogRecords.value
  const hasAgent = (agentName: string) => logs.some((log) => log.agentName === agentName)
  const imageAnalyzerLog = [...logs].reverse().find((log) => log.agentName === 'agent4_analyze_image_requirements')
  let analyzedImageCount = 0
  if (imageAnalyzerLog?.outputData) {
    try {
      analyzedImageCount = Number((JSON.parse(imageAnalyzerLog.outputData) as { requirementsCount?: number }).requirementsCount) || 0
    } catch {
      analyzedImageCount = 0
    }
  }

  if (hasAgent('agent6_merge_content') || hasAgent('content_merger')) {
    return { step: 5, statusText: '正在将配图插入正文', totalImages: analyzedImageCount }
  }
  if (hasAgent('agent5_generate_images')) {
    return { step: 5, statusText: '配图生成完成，正在合成图文', totalImages: analyzedImageCount }
  }
  if (hasAgent('agent4_analyze_image_requirements')) {
    return { step: 4, statusText: '正在生成配图', totalImages: analyzedImageCount }
  }
  if (hasAgent('agent3_generate_content')) {
    return { step: 3, statusText: '正文生成完成，正在准备分析配图', totalImages: analyzedImageCount }
  }
  return { step: 2, statusText: '正在撰写正文', totalImages: analyzedImageCount }
}

const syncTaskSnapshot = (snapshot: API.ArticleVO | undefined) => {
  if (!snapshot || (snapshot.taskId && snapshot.taskId !== taskId.value)) return

  const previousContent = article.value.content
  article.value = { ...article.value, ...snapshot }

  if (snapshot.content && snapshot.content !== previousContent) {
    scheduleStreamingMarkdown(snapshot.content)
  }
  if (snapshot.fullContent) article.value.fullContent = snapshot.fullContent
  if (snapshot.images?.length) article.value.images = snapshot.images

  // The server keeps titleOptions on the article after title confirmation.
  // Prefer the persisted phase so polling cannot move the UI back to title selection.
  if (snapshot.phase === 'OUTLINE_GENERATING') {
    currentPhase.value = 'OUTLINE_GENERATING'
    isCreating.value = true
  } else if (snapshot.phase === 'OUTLINE_EDITING' && snapshot.outline?.length) {
    if (currentPhase.value !== 'OUTLINE_EDITING') {
      handleSSEMessage({ type: 'OUTLINE_GENERATED', outline: snapshot.outline })
    }
  } else if (snapshot.phase === 'CONTENT_GENERATING') {
    currentPhase.value = 'CONTENT_GENERATING'
    const progress = getExecutionProgress()
    currentStep.value = Math.max(2, currentStep.value, progress.step)
    if (!currentStepStatusText.value || currentStep.value <= 2) {
      currentStepStatusText.value = progress.statusText
    }
    if (progress.totalImages > 0 && totalImages.value <= 0) {
      totalImages.value = progress.totalImages
    }
    isCreating.value = true
    if (snapshot.content) {
      isStreaming.value = true
      scheduleStreamingMarkdown(snapshot.content)
    }
  } else if (snapshot.phase === 'TITLE_SELECTING' && snapshot.titleOptions?.length) {
    if (currentPhase.value !== 'TITLE_SELECTING') {
      handleSSEMessage({ type: 'TITLES_GENERATED', titleOptions: snapshot.titleOptions })
    }
  } else if (snapshot.outline?.length && currentPhase.value !== 'OUTLINE_EDITING') {
    handleSSEMessage({ type: 'OUTLINE_GENERATED', outline: snapshot.outline })
  } else if (snapshot.phase === 'TITLE_GENERATING') {
    currentPhase.value = 'TITLE_GENERATING'
    isCreating.value = true
  }

  if (snapshot.status === 'COMPLETED' && !isCompleted.value) {
    handleSSEMessage({ type: 'ALL_COMPLETE' })
  } else if (snapshot.status === 'FAILED' && !errorVisible.value) {
    handleSSEMessage({ type: 'ERROR', message: snapshot.errorMessage || '创作失败' })
  }
}

const startExecutionLogsPolling = (existingTaskId: string) => {
  stopExecutionLogsPolling()
  const sync = async () => {
    if (pollingInFlight) return
    pollingInFlight = true
    if (taskId.value !== existingTaskId) {
      stopExecutionLogsPolling()
      pollingInFlight = false
      return
    }

    try {
      await loadExecutionLogs(existingTaskId)
      const response = await getArticle({ taskId: existingTaskId })
      const snapshot = response.data.data
      syncTaskSnapshot(snapshot)
      if (!isArticleRunning(snapshot?.status)) stopExecutionLogsPolling()
    } catch (error) {
      console.warn('刷新文章任务状态失败:', error)
    } finally {
      pollingInFlight = false
    }
  }

  void sync()
  executionLogsTimer = window.setInterval(async () => {
    await sync()
  }, 2000)
}

// Markdown 转 HTML
const markdownToHtml = (markdown: string | undefined) => {
  return marked(markdown || '') as string
}

// 流式正文按固定节奏刷新，避免每个 SSE 分片都重复解析整篇 Markdown。
const streamingMarkdownHtml = ref('')
let pendingStreamingMarkdown = ''
let streamingMarkdownTimer: number | undefined

const flushStreamingMarkdown = () => {
  if (streamingMarkdownTimer !== undefined) {
    window.clearTimeout(streamingMarkdownTimer)
    streamingMarkdownTimer = undefined
  }
  streamingMarkdownHtml.value = markdownToHtml(pendingStreamingMarkdown)
}

const scheduleStreamingMarkdown = (markdown: string) => {
  pendingStreamingMarkdown = markdown
  if (streamingMarkdownTimer !== undefined) return
  streamingMarkdownTimer = window.setTimeout(() => {
    streamingMarkdownTimer = undefined
    streamingMarkdownHtml.value = markdownToHtml(pendingStreamingMarkdown)
  }, 80)
}

const appendStreamingContent = (chunk: unknown) => {
  const nextChunk = String(chunk ?? '').replace(/^null(?=\S)/, '')
  if (!nextChunk || nextChunk === 'null' || nextChunk === 'undefined') return

  const currentContent = article.value.content || ''
  article.value.content = nextChunk.startsWith(currentContent)
    ? nextChunk
    : currentContent + nextChunk
  scheduleStreamingMarkdown(article.value.content)
}

const getArticleBodyContent = (
  markdown: string,
  images?: API.ImageItem[],
  mainTitle?: string,
) => {
  return mergeArticleImages(
    stripGeneratedArticleHeading(markdown, mainTitle),
    images,
  )
}

const getSummaryText = (markdown: string) => {
  return getArticleBodyContent(markdown, undefined, article.value.mainTitle)
    .replace(/^#{1,6}\s+/gm, '')
    .replace(/^\s*[-*+]\s+/gm, '')
    .replace(/`{1,3}/g, '')
    .replace(/\s+/g, ' ')
    .trim()
}

const summarizeParagraph = (paragraph: string) => {
  const sentences = paragraph.match(/[^。！？!?；;]+[。！？!?；;]?/g) || [paragraph]
  if (sentences.length === 1) return sentences[0].trim()

  const coreMarkers = [
    '核心',
    '本质',
    '关键',
    '意味着',
    '因此',
    '说明',
    '表明',
    '反映',
    '导致',
    '影响',
    '问题在于',
    '根源',
    '警示',
    '需要',
    '应当',
    '不是',
    '而是',
  ]
  const phraseFrequency = new Map<string, number>()
  const phrases = paragraph.match(/[\u4e00-\u9fff]{2,4}/g) || []
  phrases.forEach((phrase) => phraseFrequency.set(phrase, (phraseFrequency.get(phrase) || 0) + 1))

  return sentences
    .map((sentence, index) => {
      const sentencePhrases = sentence.match(/[\u4e00-\u9fff]{2,4}/g) || []
      const frequencyScore = sentencePhrases.reduce(
        (score, phrase) => score + (phraseFrequency.get(phrase) || 0),
        0,
      )
      const markerScore = coreMarkers.reduce(
        (score, marker) => score + (sentence.includes(marker) ? 3 : 0),
        0,
      )
      const positionScore = index === sentences.length - 1 ? 1.5 : index === 0 ? 0.5 : 1
      const detailPenalty = /[“”"《》]/.test(sentence) ? 1.5 : 0
      return {
        sentence: sentence.trim(),
        score: frequencyScore + markerScore + positionScore - detailPenalty,
      }
    })
    .sort((left, right) => right.score - left.score)[0].sentence
}

const articleSummary = computed(() => {
  const source = article.value.fullContent || article.value.content || ''
  const summaryText = getSummaryText(source)

  const summaryPoints = (article.value.outline || [])
    .map((item) => {
      return {
        title: item.title,
        detail: (item.points || []).filter(Boolean).slice(0, 1).join('、'),
      }
    })
    .filter((item) => item.title)
    .slice(0, 3)
  const fallbackPoints = summaryText
    ? [{ title: '文章概览', detail: summarizeParagraph(summaryText) }]
    : []
  const topic = article.value.topic ? `围绕“${article.value.topic}”` : '本文'
  const points = summaryPoints.length ? summaryPoints : fallbackPoints

  return {
    fullSummary: {
      intro: points.length
        ? `${topic}展开，文章重点梳理以下核心内容。`
        : '这篇文章暂时没有可提取的正文内容。',
      points,
    },
  }
})

// 仅在用户已经接近底部时跟随流式内容，并合并同一帧内的多次请求。
let scrollFrame: number | undefined
const scrollToBottom = () => {
  if (scrollFrame !== undefined) return
  scrollFrame = window.requestAnimationFrame(() => {
    scrollFrame = undefined
    const root = document.documentElement
    const distanceToBottom = root.scrollHeight - window.scrollY - window.innerHeight
    if (distanceToBottom > 240) return
    window.scrollTo({ top: root.scrollHeight, behavior: 'auto' })
  })
}

// 开始创作
const startCreate = async () => {
  if (!topic.value.trim()) {
    message.warning('请输入选题')
    return
  }

  if (!hasQuota.value) {
    message.error('配额不足，无法创建文章')
    return
  }

  isCreating.value = true
  stopExecutionLogsPolling()
  currentStep.value = 0
  currentStepStatus.value = 'working'
  currentStepStatusText.value = '正在创建文章任务'
  imageCount.value = 0
  imageFailedCount.value = 0
  imageProgress.value = 0
  imageGenerationStarted.value = false
  completedImagePositions.value.clear()
  realtimeLogs.value = []
  executionLogRecords.value = []
  addLog('开始创建文章任务...', 'info')

  try {
    // 创建任务
    const res = await createArticle({
      topic: topic.value,
      style: selectedStyle.value || undefined,
      enabledImageMethods:
        selectedImageMethods.value.length > 0 ? selectedImageMethods.value : undefined,
      referenceSummary:
        referenceItems.value.map((item) => `【${item.fileName}】\n${item.summary}`).join('\n\n') ||
        undefined,
    })
    const newTaskId = res.data.data
    if (!newTaskId) {
      throw new Error('创建任务失败：未返回任务ID')
    }
    taskId.value = newTaskId
    await router.replace({ path: '/create', query: { taskId: newTaskId } })
    addLog(`任务创建成功，ID: ${newTaskId}`, 'success')

    // 刷新用户信息（更新配额）
    await loginUserStore.fetchLoginUser()

    // 建立 SSE 连接
    addLog('已建立实时连接，开始生成...', 'info')
    eventSource = connectSSE(taskId.value, {
      onMessage: handleSSEMessage,
      onError: handleSSEError,
      onComplete: handleSSEComplete,
    })
    startExecutionLogsPolling(taskId.value)
  } catch (error) {
    const err = error as Error
    message.error(err.message || '创建任务失败')
    isCreating.value = false
  }
}

// 添加日志
const addLog = (message: string, level: string = 'info') => {
  realtimeLogs.value.push({
    timestamp: Date.now(),
    level,
    message,
    source: 'realtime',
  })
  // 限制日志数量，最多保留 50 条
  if (realtimeLogs.value.length > 50) {
    realtimeLogs.value.shift()
  }
}

// 格式化日志时间
const formatLogTime = (timestamp: number) => {
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', { hour12: false })
}

// 处理 SSE 消息
const handleSSEMessage = (msg: SSEMessage) => {
  switch (msg.type) {
    case 'AGENT1_COMPLETE':
      // 智能体1完成，进入标题生成阶段（显示加载）
      currentPhase.value = 'TITLE_GENERATING'
      currentStep.value = 1
      addLog('标题方案生成完成', 'success')
      break

    case 'TITLES_GENERATED':
      // 标题方案生成完成，切换到选择标题阶段
      currentPhase.value = 'TITLE_SELECTING'
      titleOptions.value = msg.titleOptions || []
      isCreating.value = false
      addLog(`生成了 ${msg.titleOptions?.length || 0} 个标题方案`, 'success')
      break

    case 'AGENT2_STREAMING':
      // 大纲流式输出（显示生成中状态）
      currentPhase.value = 'OUTLINE_GENERATING'
      isOutlineStreaming.value = true
      outlineRaw.value += msg.content || ''
      scheduleOutlinePreview()
      scrollToBottom()
      break

    case 'OUTLINE_GENERATED':
      // 大纲生成完成，切换到编辑大纲阶段
      currentPhase.value = 'OUTLINE_EDITING'
      outline.value = msg.outline || []
      outlineRaw.value = outline.value.length ? JSON.stringify({ sections: outline.value }) : ''
      flushOutlinePreview()
      isCreating.value = false
      isOutlineStreaming.value = false
      addLog('大纲生成完成，等待确认', 'success')
      // 保持在步骤1（规划大纲），用户编辑大纲时仍处于此阶段
      break

    case 'AGENT2_COMPLETE':
      // 大纲完成（内部处理，已在 OUTLINE_GENERATED 中切换阶段）
      // 不改变 currentStep，保持在步骤1，等用户确认大纲后才进入步骤2
      break

    case 'AGENT3_STREAMING':
      // 正文流式输出，进入步骤2（撰写正文）
      currentPhase.value = 'CONTENT_GENERATING'
      currentStep.value = 2
      if (!isStreaming.value) {
        currentStepStatus.value = 'working'
        currentStepStatusText.value = '正在撰写正文'
        addLog('正在撰写正文', 'info')
      }
      isStreaming.value = true
      appendStreamingContent(msg.content)
      scrollToBottom()
      break

    case 'AGENT3_COMPLETE':
      // 正文完成，进入配图分析步骤
      isStreaming.value = false
      flushStreamingMarkdown()
      currentStep.value = 3
      currentStepStatus.value = 'working'
      currentStepStatusText.value = '正在准备分析配图'
      addLog('正文生成完成', 'success')
      break

    case 'AGENT4_START':
      currentStep.value = 3
      currentStepStatus.value = 'working'
      currentStepStatusText.value = '正在生成配图'
      addLog('开始分析配图需求与插入位置', 'info')
      break

    case 'AGENT4_COMPLETE':
      // 配图分析完成，进入配图生成步骤
      currentStep.value = 4
      totalImages.value = msg.imageRequirements?.length || 0
      imageCount.value = 0
      imageFailedCount.value = 0
      imageProgress.value = 0
      imageGenerationStarted.value = false
      completedImagePositions.value.clear()
      currentStepStatus.value = 'working'
      currentStepStatusText.value = totalImages.value > 0 ? `准备生成 ${totalImages.value} 张配图` : '未生成配图需求'
      addLog(`配图需求分析完成，共 ${totalImages.value} 张`, 'success')
      break

    case 'IMAGE_START': {
      currentStep.value = 4
      currentStepStatus.value = 'working'
      if (!imageGenerationStarted.value) {
        imageGenerationStarted.value = true
        currentStepStatusText.value = `已开启异步任务，正在并行生成 ${totalImages.value} 张配图`
        addLog(`已开启异步任务，正在并行生成 ${totalImages.value} 张配图`, 'info')
      }
      break
    }

    case 'IMAGE_COMPLETE':
      // 单张配图完成
      {
        const { position, methodLabel } = getImageEventDetails(msg.image)
        if (!completedImagePositions.value.has(position)) {
          completedImagePositions.value.add(position)
          imageCount.value = completedImagePositions.value.size
        }
        imageProgress.value = totalImages.value > 0
          ? Math.min(100, Math.round((imageCount.value / totalImages.value) * 100))
          : 0
        currentStepStatus.value = 'working'
        currentStepStatusText.value = `已生成 ${imageCount.value}/${totalImages.value} 张配图`
        addLog(`已生成第 ${imageCount.value} 张配图${methodLabel ? ` · ${methodLabel}` : ''}`, 'success')
      }
      break

    case 'IMAGE_FAILED': {
      imageFailedCount.value++
      const { position, methodLabel } = getImageEventDetails(msg.image)
      currentStepStatus.value = 'failed'
      currentStepStatusText.value = `第 ${position}/${totalImages.value} 张配图生成失败`
      addLog(`第 ${position}/${totalImages.value} 张配图生成失败${methodLabel ? ` · ${methodLabel}` : ''}`, 'error')
      break
    }

    case 'IMAGE_SKIPPED':
      // 未插入正文的图片属于封面或无匹配占位符图片，不在用户执行日志中展示。
      break

    case 'AGENT5_COMPLETE':
      // 所有配图完成，进入图文合成步骤
      currentStep.value = 5
      article.value.images = msg.images
      currentStepStatus.value = 'working'
      currentStepStatusText.value = '正在将配图插入正文'
      addLog('所有配图生成完成', 'success')
      break

    case 'MERGE_START':
      currentStep.value = 5
      currentStepStatus.value = 'working'
      currentStepStatusText.value = '正在将配图插入正文'
      addLog(
        `开始图文合成：选取 ${Number(msg.bodyImages || 0)} 张正文配图${
          Number(msg.coverImages || 0) > 0
            ? `（封面 ${Number(msg.coverImages)} 张不参与正文合成）`
            : ''
        }`,
        'info',
      )
      break

    case 'MERGE_COMPLETE':
      // 图文合成完成
      article.value.fullContent = msg.fullContent
      flushStreamingMarkdown()
      currentStepStatus.value = 'working'
      currentStepStatusText.value = '图文合成完成'
      scrollToBottom()
      addLog('图文合成完成', 'success')
      break

    case 'ALL_COMPLETE':
      // 全部完成
      currentPhase.value = 'COMPLETED'
      currentStep.value = 6
      currentStepStatus.value = 'working'
      currentStepStatusText.value = '文章创作完成后即可'
      isCompleted.value = true
      void syncCompletedArticle()
      message.success('文章创作完成!')
      break

    case 'ERROR':
      errorMessage.value = msg.message || '创作失败'
      errorVisible.value = true
      isCreating.value = false
      currentStepStatus.value = 'failed'
      currentStepStatusText.value = '创作失败'
      currentPhase.value = 'INPUT'
      addLog(`创作失败: ${msg.message || '未知错误'}`, 'error')
      break
  }
}

const syncCompletedArticle = async () => {
  if (!taskId.value) return

  try {
    const response = await getArticle({ taskId: taskId.value })
    const completedArticle = response.data.data
    if (completedArticle?.fullContent) {
      article.value = { ...article.value, ...completedArticle }
    }
  } catch (error) {
    console.warn('获取最终图文内容失败', error)
  }
}

const restoreArticleForEditing = async (existingTaskId: string) => {
  try {
    const response = await getArticle({ taskId: existingTaskId })
    const existingArticle = response.data.data
    if (!existingArticle) {
      message.error('文章不存在或已被删除')
      return
    }

    taskId.value = existingTaskId
    stopExecutionLogsPolling()
    await loadExecutionLogs(existingTaskId)
    topic.value = existingArticle.topic || ''
    titleOptions.value = (existingArticle.titleOptions || [])
      .filter((item) => item.mainTitle && item.subTitle)
      .map((item) => ({ mainTitle: item.mainTitle as string, subTitle: item.subTitle as string }))
    outline.value = (existingArticle.outline || [])
      .filter((item) => item.section !== undefined && item.title && item.points)
      .map((item) => ({
        section: item.section as number,
        title: item.title as string,
        points: item.points as string[],
      }))
    outlineRaw.value = outline.value.length ? JSON.stringify({ sections: outline.value }) : ''
    flushOutlinePreview()
    article.value = { ...article.value, ...existingArticle }
    pendingStreamingMarkdown = existingArticle.content || ''
    flushStreamingMarkdown()

    if (existingArticle.status === 'COMPLETED' || existingArticle.phase === 'COMPLETED') {
      currentPhase.value = 'COMPLETED'
      isCompleted.value = true
      currentStep.value = agentSteps.length
      currentStepStatusText.value = '文章创作完成后即可'
    } else if (existingArticle.phase === 'PENDING' || existingArticle.phase === 'TITLE_GENERATING') {
      currentPhase.value = 'TITLE_GENERATING'
      currentStep.value = 0
      currentStepStatus.value = 'working'
      currentStepStatusText.value = '正在生成标题方案'
      isCreating.value = true
    } else if (existingArticle.phase === 'OUTLINE_EDITING') {
      currentPhase.value = 'OUTLINE_EDITING'
      currentStep.value = 1
      currentStepStatus.value = 'waiting'
      currentStepStatusText.value = '等待您的确认...'
    } else if (existingArticle.phase === 'OUTLINE_GENERATING') {
      currentPhase.value = 'OUTLINE_GENERATING'
      currentStep.value = 1
      currentStepStatus.value = 'working'
      currentStepStatusText.value = '正在生成文章大纲'
      isCreating.value = true
    } else if (existingArticle.phase === 'CONTENT_GENERATING') {
      currentPhase.value = 'CONTENT_GENERATING'
      const progress = getExecutionProgress()
      currentStep.value = progress.step
      totalImages.value = progress.totalImages || existingArticle.images?.length || 0
      imageCount.value = existingArticle.images?.length || 0
      imageProgress.value = totalImages.value > 0
        ? Math.min(100, Math.round((imageCount.value / totalImages.value) * 100))
        : 0
      currentStepStatusText.value = progress.statusText
      isCreating.value = true
    } else if (existingArticle.phase === 'TITLE_SELECTING') {
      currentPhase.value = 'TITLE_SELECTING'
      currentStep.value = 0
      currentStepStatus.value = 'waiting'
      currentStepStatusText.value = '等待您的确认...'
    } else {
      currentPhase.value = 'INPUT'
    }

    if (isArticleRunning(existingArticle.status)) {
      eventSource = connectSSE(existingTaskId, {
        onMessage: handleSSEMessage,
        onError: handleSSEError,
        onComplete: handleSSEComplete,
      })
      startExecutionLogsPolling(existingTaskId)
    }
  } catch (error) {
    message.error((error as Error).message || '加载文章失败')
  }
}

// 确认标题
const handleConfirmTitle = async (data: {
  mainTitle: string
  subTitle: string
  userDescription: string
}) => {
  confirmLoading.value = true
  try {
    await confirmTitle({
      taskId: taskId.value,
      selectedMainTitle: data.mainTitle,
      selectedSubTitle: data.subTitle,
      userDescription: data.userDescription,
    })
    // 保存标题信息，用于大纲生成阶段展示
    article.value.mainTitle = data.mainTitle
    article.value.subTitle = data.subTitle
    // 不直接切换阶段，等待 SSE 消息 OUTLINE_GENERATED
    message.success('标题已确认，正在生成大纲...')
  } catch (error) {
    const err = error as Error
    message.error(err.message || '确认标题失败')
  } finally {
    confirmLoading.value = false
  }
}

// 确认大纲
const handleConfirmOutline = async (
  outlineData: Array<{ section: number; title: string; points: string[] }>,
) => {
  confirmLoading.value = true
  try {
    await confirmOutline({
      taskId: taskId.value,
      outline: outlineData,
    })
    // 更新 outlineRaw 为用户修改后的大纲，确保 CONTENT_GENERATING 阶段展示正确的大纲
    outlineRaw.value = JSON.stringify({ sections: outlineData })
    flushOutlinePreview()
    // 不直接切换阶段，等待后端开始生成正文并推送 AGENT3_STREAMING
    message.success('大纲已确认，正在生成正文...')
  } catch (error) {
    const err = error as Error
    message.error(err.message || '确认大纲失败')
  } finally {
    confirmLoading.value = false
  }
}

// 处理 SSE 错误
const handleSSEError = (error: Event) => {
  console.error('SSE错误:', error)
  if (!pollingFallbackActive && taskId.value) {
    pollingFallbackActive = true
    addLog('实时连接暂时中断，已切换为状态同步', 'info')
    startExecutionLogsPolling(taskId.value)
  }
}

// 处理 SSE 完成
const handleSSEComplete = () => {
  // SSE 在 ALL_COMPLETE 或 ERROR 后由连接工具主动关闭。
}

const copyTextWithFallback = (text: string) => {
  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.setAttribute('readonly', '')
  textarea.style.position = 'fixed'
  textarea.style.top = '0'
  textarea.style.left = '-9999px'
  textarea.style.opacity = '0'
  document.body.appendChild(textarea)

  try {
    textarea.focus()
    textarea.select()
    textarea.setSelectionRange(0, text.length)
    return document.execCommand('copy')
  } finally {
    document.body.removeChild(textarea)
  }
}

// 复制全文
const copyContent = async () => {
  const content = mergeArticleImages(
    article.value.fullContent || article.value.content || '',
    article.value.images,
  )
  try {
    let copied = false
    if (window.isSecureContext && navigator.clipboard?.writeText) {
      try {
        await navigator.clipboard.writeText(content)
        copied = true
      } catch {
        // 浏览器权限限制下，继续使用兼容方案。
      }
    }
    if (!copied) copied = copyTextWithFallback(content)
    if (!copied) throw new Error('Copy command failed')
    message.success('已复制到剪贴板')
  } catch {
    message.error('复制失败')
  }
}

// 查看文章详情
const viewArticle = () => {
  router.push(`/article/${taskId.value}`)
}

const exportArticle = () => {
  exportAsMarkdown({
    title: article.value.mainTitle || '文章',
    subTitle: article.value.subTitle,
    content: article.value.content,
    fullContent: article.value.fullContent,
    outline: article.value.outline?.map((item, index) => ({
      section: item.section ?? index + 1,
      title: item.title || '',
    })),
    images: article.value.images?.map((image) => ({
      description: image.description || '',
      url: image.url || '',
      sectionTitle: image.sectionTitle,
      position: image.position,
      placeholderId: image.placeholderId,
    })),
  })
  message.success('导出成功')
}

// 重新创作
const startArticleEdit = () => {
  editableContent.value = getEditableArticleContent(article.value)
  editableMainTitle.value = article.value.mainTitle || ''
  editableSubTitle.value = article.value.subTitle || ''
  aiEditMode.value = 'content'
  aiEditInstruction.value = ''
  contentAiInstruction.value = ''
  imageAiInstruction.value = ''
  aiEditImagePosition.value = article.value.images?.[0]?.position
  editActivityLogs.value = []
  addEditActivityLog('已进入编辑模式，可修改正文和配图', 'info')
  isEditingArticle.value = true
}

const cancelArticleEdit = () => {
  isEditingArticle.value = false
  editableContent.value = ''
  editableMainTitle.value = ''
  editableSubTitle.value = ''
  aiEditInstruction.value = ''
  contentAiInstruction.value = ''
  imageAiInstruction.value = ''
  editActivityLogs.value = []
}

const addEditActivityLog = (
  message: string,
  status: EditActivityLog['status'] = 'info',
) => {
  editActivityLogs.value.unshift({
    id: Date.now() + editActivityLogs.value.length,
    timestamp: Date.now(),
    message,
    status,
  })
  if (editActivityLogs.value.length > 12) {
    editActivityLogs.value.pop()
  }
}

const settleLatestEditActivityLog = (
  message: string,
  status: Extract<EditActivityLog['status'], 'success' | 'error'>,
) => {
  const processingLog = editActivityLogs.value.find((log) => log.status === 'processing')
  if (processingLog) {
    processingLog.message = message
    processingLog.status = status
    return
  }
  addEditActivityLog(message, status)
}

const runAiEdit = async () => {
  if (!taskId.value || !aiEditInstruction.value.trim()) {
    message.warning('请输入 AI 修改要求')
    return
  }

  if (aiEditMode.value === 'image') {
    if (editableContent.value !== getEditableArticleContent(article.value)) {
      message.warning('请先保存文本修改，再让 AI 优化配图')
      return
    }
    if (!aiEditImagePosition.value) {
      message.warning('请选择要优化的配图')
      return
    }
  }

  aiEditing.value = true
  try {
    if (aiEditMode.value === 'content') {
      addEditActivityLog('正在提交正文修改请求', 'processing')
      const response = await aiEditArticleContent({
        taskId: taskId.value,
        content: editableContent.value,
        instruction: aiEditInstruction.value.trim(),
      })
      const generatedContent = response.data.data
      if (!generatedContent) throw new Error(response.data.message || 'AI 内容修改失败')
      editableContent.value = generatedContent
      settleLatestEditActivityLog('AI 已完成正文修改，请确认后保存', 'success')
      message.success('AI 已生成内容修改，请确认后保存')
    } else {
      addEditActivityLog(`正在优化第 ${aiEditImagePosition.value} 张配图`, 'processing')
      const response = await regenerateArticleImage({
        taskId: taskId.value,
        position: aiEditImagePosition.value,
        prompt: aiEditInstruction.value.trim(),
      }, { timeout: 120_000 })
      const updatedArticle = response.data.data
      if (!updatedArticle) throw new Error(response.data.message || '配图优化失败')
      article.value = { ...article.value, ...updatedArticle }
      editableContent.value = getEditableArticleContent(updatedArticle)
      settleLatestEditActivityLog(`第 ${aiEditImagePosition.value} 张配图已优化并替换`, 'success')
      message.success('AI 已优化并替换配图')
    }
    aiEditInstruction.value = ''
  } catch (error) {
    settleLatestEditActivityLog((error as Error).message || 'AI 编辑失败，请稍后重试', 'error')
    message.error((error as Error).message || 'AI 编辑失败')
  } finally {
    aiEditing.value = false
  }
}

const runContentAiEdit = async () => {
  aiEditMode.value = 'content'
  aiEditInstruction.value = contentAiInstruction.value
  await runAiEdit()
  contentAiInstruction.value = aiEditInstruction.value
}

const runImageAiEdit = async () => {
  aiEditMode.value = 'image'
  aiEditInstruction.value = imageAiInstruction.value
  await runAiEdit()
  imageAiInstruction.value = aiEditInstruction.value
}

const saveArticleEdit = async () => {
  if (!taskId.value || !editableMainTitle.value.trim() || !editableContent.value.trim()) {
    message.warning('文章内容不能为空')
    return
  }

  articleSaving.value = true
  try {
    addEditActivityLog('正在保存文章修改', 'processing')
    await updateArticleContent({
      taskId: taskId.value,
      mainTitle: editableMainTitle.value.trim(),
      subTitle: editableSubTitle.value.trim(),
      content: editableContent.value,
    })
    article.value.mainTitle = editableMainTitle.value.trim()
    article.value.subTitle = editableSubTitle.value.trim()
    article.value.fullContent = editableContent.value
    settleLatestEditActivityLog('文章修改已保存', 'success')
    isEditingArticle.value = false
    message.success('文章修改已保存')
  } catch (error) {
    settleLatestEditActivityLog((error as Error).message || '保存文章修改失败，请重试', 'error')
    message.error((error as Error).message || '保存文章修改失败')
  } finally {
    articleSaving.value = false
  }
}

const openImageEditor = (image: API.ImageItem) => {
  if (editableContent.value !== getEditableArticleContent(article.value)) {
    message.warning('请先保存文本修改，再重新生成图片')
    return
  }
  editingImage.value = image
  imageEditVisible.value = true
}

const selectImageVersion = async (versionId?: string) => {
  if (!taskId.value || !editingImage.value?.position || !versionId || versionId === editingImage.value.selectedVersionId) {
    return
  }
  imageSwitching.value = true
  try {
    const response = await selectArticleImageVersion({
      taskId: taskId.value,
      position: editingImage.value.position,
      versionId,
    })
    const updatedArticle = response.data.data
    if (!updatedArticle) throw new Error(response.data.message || '图片切换失败')
    article.value = { ...article.value, ...updatedArticle }
    editableContent.value = getEditableArticleContent(updatedArticle)
    editingImage.value = updatedArticle.images?.find(item => item.position === editingImage.value?.position) || null
    message.success('已切换图片版本')
  } catch (error) {
    message.error((error as Error).message || '图片切换失败')
  } finally {
    imageSwitching.value = false
  }
}

const resetCreate = () => {
  closeSSE(eventSource)
  eventSource = null
  stopExecutionLogsPolling()
  void router.replace({ path: '/create' })
  currentPhase.value = 'INPUT'
  topic.value = ''
  clearReference()
  selectedStyle.value = ''
  titleOptions.value = []
  outline.value = []
  isCreating.value = false
  isCompleted.value = false
  isStreaming.value = false
  isOutlineStreaming.value = false
  currentStep.value = 0
  imageCount.value = 0
  imageFailedCount.value = 0
  imageProgress.value = 0
  imageGenerationStarted.value = false
  completedImagePositions.value.clear()
  currentStepStatus.value = 'working'
  currentStepStatusText.value = ''
  outlineRaw.value = ''
  flushOutlinePreview()
  pendingStreamingMarkdown = ''
  flushStreamingMarkdown()
  confirmLoading.value = false
  realtimeLogs.value = []
  executionLogRecords.value = []
  article.value = {
    mainTitle: '',
    subTitle: '',
    content: '',
    fullContent: '',
    images: [],
  }
  cancelArticleEdit()
}

const openReferencePicker = () => {
  referenceFileInput.value?.click()
}

const handleReferenceFileChange = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files || [])
  if (!files.length) return

  referenceError.value = ''
  isReferenceParsing.value = true
  try {
    for (const file of files) {
      const response = await parseArticleReference(file)
      const reference = response.data.data
      if (!reference?.summary) {
        throw new Error(response.data.message || `${file.name} 摘要为空`)
      }
      referenceItems.value.push({
        id: Date.now() + referenceItems.value.length,
        fileName: reference.fileName || file.name,
        summary: reference.summary,
        characterCount: reference.characterCount || 0,
        expanded: true,
      })
    }
    message.success(`已解析 ${files.length} 份参考材料`)
  } catch (error) {
    referenceError.value = (error as Error).message || '文档解析失败，请重试'
  } finally {
    isReferenceParsing.value = false
    input.value = ''
  }
}

const clearReference = (id?: number) => {
  if (id === undefined) {
    referenceItems.value = []
  } else {
    referenceItems.value = referenceItems.value.filter((item) => item.id !== id)
  }
  referenceError.value = ''
  if (referenceFileInput.value) {
    referenceFileInput.value.value = ''
  }
}

const handleFeedbackImages = (event: Event) => {
  const input = event.target as HTMLInputElement
  const selectedImages = Array.from(input.files || [])
  const allowedTypes = new Set(['image/jpeg', 'image/png', 'image/webp'])
  const validImages = selectedImages.filter((image) => {
    if (!allowedTypes.has(image.type)) {
      message.warning(`${image.name} 不是支持的图片格式`)
      return false
    }
    if (image.size > 5 * 1024 * 1024) {
      message.warning(`${image.name} 超过 5MB 限制`)
      return false
    }
    return true
  })
  const remaining = 3 - feedbackImages.value.length
  if (validImages.length > remaining) {
    message.warning('最多上传 3 张图片')
  }
  feedbackImages.value.push(...validImages.slice(0, Math.max(remaining, 0)))
  input.value = ''
}

const removeFeedbackImage = (image: File) => {
  feedbackImages.value = feedbackImages.value.filter((item) => item !== image)
}

const handleFeedbackSubmit = async () => {
  if (!feedbackContent.value.trim()) {
    message.warning('请填写反馈建议')
    return
  }
  feedbackSubmitting.value = true
  try {
    const response = await submitFeedback(feedbackContent.value.trim(), feedbackImages.value)
    if (response.data.code !== 0 || !response.data.data) {
      throw new Error(response.data.message || '反馈发送失败，请稍后重试')
    }
    message.success('反馈已提交，感谢你的建议')
    feedbackContent.value = ''
    feedbackImages.value = []
    feedbackVisible.value = false
  } catch (error) {
    message.error((error as Error).message || '反馈发送失败，请稍后重试')
  } finally {
    feedbackSubmitting.value = false
  }
}

// 组件挂载时检查路由参数
onMounted(() => {
  if (route.query.taskId) {
    void restoreArticleForEditing(String(route.query.taskId))
  } else if (route.query.topic) {
    topic.value = route.query.topic as string
    window.setTimeout(animateCurrentStage, 80)
  } else {
    window.setTimeout(animateCurrentStage, 80)
  }
  startHotTopicLoop()
  loadHotTopics()
})

const loadHotTopics = async (refresh = false) => {
  hotTopicsLoading.value = true
  try {
    const res = await getHotTopics(refresh ? { params: { refresh: true } } : undefined)
    const data = res.data.data
    if (data?.items?.length) {
      hotTopics.value = data.items
      hotTopicsSource.value = data.source || 'gnews'
      hotTopicsUpdatedAt.value = data.updatedAt || ''
      hotTopicOffset.value = 0
      startHotTopicLoop()
    }
  } catch (error) {
    console.warn('加载热门选题失败，继续使用推荐选题', error)
  } finally {
    hotTopicsLoading.value = false
  }
}

const formatHotTopicsTime = (value: string) => {
  if (!value) return ''
  return new Date(value).toLocaleTimeString('zh-CN', { hour12: false })
}

const selectHotTopic = (item: API.HotTopicItem) => {
  if (item.title) topic.value = item.title
}

// 组件卸载前关闭 SSE
onBeforeUnmount(() => {
  closeSSE(eventSource)
  stopExecutionLogsPolling()
  stopHotTopicLoop()
  if (outlinePreviewTimer !== undefined) window.clearTimeout(outlinePreviewTimer)
  if (streamingMarkdownTimer !== undefined) window.clearTimeout(streamingMarkdownTimer)
  if (scrollFrame !== undefined) window.cancelAnimationFrame(scrollFrame)
  stageTimeline?.kill()
  stageTimeline = null
})
</script>

<style scoped lang="scss">
.article-create-page {
  height: auto;
  margin-bottom: 0;
  background: var(--color-background-secondary);
  overflow-x: hidden;
  overflow-y: auto;
}

.create-layout {
  display: grid;
  grid-template-columns: minmax(260px, 300px) minmax(0, 1fr) minmax(250px, 300px);
  min-height: 100%;
  height: auto;
}

/* 左侧边栏 */
.sidebar-left {
  background: white;
  border-right: 1px solid var(--color-border);
  padding: 24px;
  display: flex;
  flex-direction: column;
  overflow: visible;
}

.sidebar-header {
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--color-border-light);
}

.sidebar-title {
  font-size: 18px;
  font-weight: 700;
  margin: 0 0 4px;
  color: var(--color-text);
}

.sidebar-subtitle {
  font-size: 13px;
  color: var(--color-text-muted);
  margin: 0;
}

.flow-timeline {
  flex: 0 0 auto;
}

.article-summary-panel {
  flex: 0 0 auto;
  margin-top: 20px;
  padding: 18px 14px 16px;
  border-top: 1px solid var(--line-soft);
}

.article-summary-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.article-summary-heading {
  margin-bottom: 14px;
  color: var(--ink-deep);

  .anticon {
    flex: 0 0 auto;
    color: var(--river-green);
    font-size: 18px;
  }

  h4 {
    margin: 2px 0 0;
    color: var(--ink-deep);
    font-size: 16px;
    font-weight: 600;
  }
}

.article-summary-kicker,
.article-summary-label {
  color: var(--river-green);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.article-summary-overview {
  margin-bottom: 14px;
  padding: 10px 11px;
  border-left: 3px solid var(--river-green);
  background: rgba(236, 244, 237, 0.78);

  .article-summary-intro {
    margin: 5px 0 0;
    color: var(--ink-deep);
    font-size: 12px;
    font-weight: 600;
    line-height: 1.7;
    overflow-wrap: anywhere;
  }
}

.article-summary-points {
  display: flex;
  flex-direction: column;
  gap: 9px;
  margin-top: 11px;
}

.article-summary-point {
  display: grid;
  grid-template-columns: 22px minmax(0, 1fr);
  gap: 8px;
  padding-top: 9px;
  border-top: 1px solid rgba(69, 111, 100, 0.12);
}

.article-summary-point-index {
  padding-top: 1px;
  color: var(--river-green);
  font-family: 'Outfit', 'Microsoft YaHei', sans-serif;
  font-size: 10px;
  font-weight: 700;
}

.article-summary-point-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;

  strong {
    color: var(--color-text);
    font-size: 12px;
    line-height: 1.5;
    overflow-wrap: anywhere;
  }

  span {
    color: var(--color-text-secondary);
    font-size: 11px;
    line-height: 1.6;
    overflow-wrap: anywhere;
  }
}

.flow-item {
  display: flex;
  gap: 14px;
  padding: 14px 0;
  position: relative;

  &:not(:last-child)::before {
    content: '';
    position: absolute;
    left: 15px;
    top: 46px;
    bottom: -14px;
    width: 2px;
    background: var(--color-border);
  }

  &.completed::before {
    background: var(--color-primary);
  }

  &.active::before {
    background: linear-gradient(180deg, var(--color-primary) 50%, var(--color-border) 50%);
  }

  &.failed::before {
    background: linear-gradient(180deg, #d86f58 50%, var(--color-border) 50%);
  }
}

.flow-indicator {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 14px;
  transition: all var(--transition-normal);

  .pending & {
    background: var(--color-background-tertiary);
    color: var(--color-text-muted);
    border: 2px solid var(--color-border);
  }

  .active & {
    background: rgba(34, 197, 94, 0.1);
    color: var(--color-primary);
    border: 2px solid var(--color-primary);
  }

  .completed & {
    background: var(--color-primary);
    color: white;
  }

  .failed & {
    background: rgba(216, 111, 88, 0.12);
    color: #b84d39;
    border: 2px solid #d86f58;
  }

  .step-number {
    font-weight: 600;
  }

  .spin-icon {
    animation: spin 1s linear infinite;
  }
}

.flow-content {
  flex: 1;
  min-width: 0;
}

.flow-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 2px;

  .pending & {
    color: var(--color-text-muted);
  }

  .active & {
    color: var(--color-primary-dark);
  }
}

.flow-desc {
  font-size: 12px;
  color: var(--color-text-muted);
  line-height: 1.4;
}

.flow-status {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
  font-size: 12px;
  color: var(--color-primary);
  font-weight: 500;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-primary);
  animation: pulse 1.5s infinite;

  &.failed {
    background: #d86f58;
    animation: none;
  }
}

.completed-status {
  color: var(--color-text-muted);
  font-size: 11px;
}

/* 主内容区 */
.main-content {
  min-width: 0;
  padding: 32px 40px;
  overflow: visible;
  background: white;
}

/* 输入状态 */
.input-state {
  max-width: 700px;
  margin: 0 auto;
  padding-top: 60px;
}

.input-card {
  background: var(--color-background-secondary);
  border-radius: var(--radius-xl);
  padding: 40px;
}

.input-header {
  text-align: center;
  margin-bottom: 32px;
}

.input-title {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 8px;
  color: var(--color-text);
}

.input-subtitle {
  font-size: 15px;
  color: var(--color-text-secondary);
  margin: 0;
}

.input-area {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.topic-textarea {
  font-size: 15px;
  border-radius: var(--radius-lg);
  padding: 16px;
  background: white;

  &:focus {
    border-color: var(--color-primary);
    box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.1);
  }
}

.reference-upload {
  padding: 16px;
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.68);
}

.reference-upload-header,
.reference-summary-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.reference-upload-header .section-tip {
  display: block;
  margin-top: 4px;
}

.reference-file-input {
  display: none;
}

.reference-status,
.reference-empty {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  color: var(--color-text-secondary);
  font-size: 13px;
}

.reference-summary {
  margin-top: 12px;
  padding: 12px;
  border-radius: var(--radius-md);
  background: rgba(34, 197, 94, 0.06);
}

.reference-list {
  display: grid;
  gap: 10px;
}

.reference-list .reference-summary {
  margin-top: 0;
}

.reference-summary-meta {
  color: var(--color-text-muted);
  font-size: 12px;
}

.reference-actions {
  display: inline-flex;
  align-items: center;
  flex: 0 0 auto;
  gap: 2px;
  margin-left: auto;
}

.reference-actions .ant-btn {
  width: 28px;
  height: 28px;
  padding: 0;
  color: var(--color-text-secondary);
}

.reference-actions .ant-btn:hover {
  color: var(--color-primary);
  background: rgba(34, 197, 94, 0.08);
}

.reference-actions .ant-btn-dangerous:hover {
  color: #d4380d;
  background: rgba(212, 56, 13, 0.08);
}

.reference-file-name {
  display: inline-flex;
  align-items: center;
  min-width: 0;
  gap: 6px;
  color: var(--color-text);
  font-weight: 600;
}

.reference-file-name :deep(svg) {
  flex: 0 0 auto;
}

.reference-summary p {
  margin: 10px 0 0;
  color: var(--color-text-secondary);
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
}

.reference-error {
  margin-top: 10px;
  color: #d4380d;
  font-size: 12px;
  line-height: 1.5;
}

.create-btn.ant-btn {
  height: 52px;
  font-size: 16px;
  font-weight: 600;
  border-radius: var(--radius-lg);
  background: var(--gradient-primary) !important;
  border: none !important;
  color: white !important;
  box-shadow: 0 4px 14px rgba(34, 197, 94, 0.3) !important;

  &:hover,
  &:focus,
  &:active {
    background: var(--gradient-primary) !important;
    color: white !important;
    border: none !important;
    box-shadow: 0 4px 14px rgba(34, 197, 94, 0.3) !important;
    opacity: 0.92;
  }

  &:disabled,
  &.ant-btn-disabled {
    background: var(--color-border) !important;
    box-shadow: none !important;
    opacity: 0.6;
    color: var(--color-text-muted) !important;
  }
}

.quota-warning {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 12px;
  padding: 10px 16px;
  background: rgba(255, 77, 79, 0.08);
  border: 1px solid rgba(255, 77, 79, 0.2);
  border-radius: var(--radius-md);
  color: #ff4d4f;
  font-size: 13px;
}

/* 文章风格选择 */
.style-section {
  padding: 16px;
  background: var(--color-background-secondary);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border-light);
}

.style-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.style-group :deep(.ant-radio-wrapper) {
  margin: 0;
  padding: 6px 12px;
  background: white;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  transition: all 0.2s;
}

.style-group :deep(.ant-radio-wrapper:hover) {
  border-color: var(--color-primary);
  background: rgba(34, 197, 94, 0.04);
}

.style-group :deep(.ant-radio-wrapper-checked) {
  border-color: var(--color-primary);
  background: rgba(34, 197, 94, 0.08);
}

/* 配图方式选择 */
.image-methods-section {
  padding: 16px;
  background: var(--color-background-secondary);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border-light);
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
}

.section-tip {
  font-size: 12px;
  color: var(--color-text-muted);
}

.methods-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.methods-group :deep(.ant-checkbox-wrapper) {
  margin: 0;
  padding: 6px 12px;
  background: white;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  transition: all 0.2s;
}

.methods-group :deep(.ant-checkbox-wrapper:hover) {
  border-color: var(--color-primary);
  background: rgba(34, 197, 94, 0.04);
}

.methods-group :deep(.ant-checkbox-wrapper-checked) {
  border-color: var(--color-primary);
  background: rgba(34, 197, 94, 0.08);
}

.methods-group :deep(.ant-checkbox-wrapper-disabled) {
  opacity: 0.6;
  cursor: not-allowed;
}

.vip-icon {
  color: var(--color-primary);
  font-size: 12px;
  margin-left: 4px;
}

.vip-notice {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
  padding: 10px 14px;
  background: rgba(34, 197, 94, 0.08);
  border-radius: var(--radius-md);
  font-size: 12px;
  color: var(--color-primary-dark);
  border: 1px solid rgba(34, 197, 94, 0.2);

  .anticon {
    color: var(--color-primary);
  }

  .upgrade-link {
    color: var(--color-primary);
    font-weight: 600;
    text-decoration: none;

    &:hover {
      text-decoration: underline;
    }
  }
}

/* 创作进行中 */
.creating-state,
.completed-state {
  max-width: 100%;
}

/* 标题区域 */
.preview-header {
  text-align: center;
  margin-bottom: 24px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--color-border-light);
}

.article-title {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 8px;
  color: var(--color-text);
  line-height: 1.4;
}

.article-subtitle {
  font-size: 16px;
  color: var(--color-text-secondary);
  margin: 0;
}

/* 大纲预览 */
.outline-preview {
  margin-bottom: 24px;
  padding: 20px 24px;
  background: var(--color-background-secondary);
  border-radius: var(--radius-lg);
}

.section-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-primary);
  margin-bottom: 16px;
}

.outline-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.outline-item {
  padding: 12px 16px;
  background: white;
  border-radius: var(--radius-md);
  border-left: 3px solid var(--color-primary);
}

.outline-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 8px;
}

.outline-points {
  margin: 0;
  padding-left: 18px;

  li {
    font-size: 13px;
    color: var(--color-text-secondary);
    line-height: 1.6;
    margin-bottom: 4px;

    &:last-child {
      margin-bottom: 0;
    }
  }
}

/* 正文预览 */
.content-preview {
  line-height: 1.8;
}

.article-editor {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.article-editor-textarea {
  min-height: 480px;
  line-height: 1.8;
}

.image-editor-section {
  padding-top: 4px;
}

.image-editor-heading {
  margin-bottom: 10px;
  font-weight: 600;
}

.image-editor-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
}

.image-editor-item {
  overflow: hidden;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
}

.image-editor-item img {
  display: block;
  width: 100%;
  height: 120px;
  object-fit: cover;
}

.image-editor-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px;
  font-size: 12px;
}

.image-editor-meta span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.image-version-picker-title {
  margin-bottom: 2px;
  color: var(--color-text);
  font-size: 13px;
  font-weight: 600;
}

.image-version-picker-hint {
  margin-bottom: 10px;
  color: var(--color-text-muted);
  font-size: 12px;
}

.image-version-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(132px, 1fr));
  gap: 10px;
  margin-bottom: 4px;
}

.image-version-option {
  position: relative;
  padding: 0;
  overflow: hidden;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text);
  cursor: pointer;
  text-align: left;
}

.image-version-option.selected {
  border-color: var(--river-green);
  box-shadow: 0 0 0 2px rgba(69, 111, 100, 0.14);
}

.image-version-option:disabled {
  cursor: wait;
  opacity: 0.64;
}

.image-version-option img {
  display: block;
  width: 100%;
  height: 92px;
  object-fit: cover;
}

.image-version-option span {
  display: block;
  padding: 6px 8px;
  font-size: 12px;
}

.image-version-check {
  position: absolute;
  top: 6px;
  right: 6px;
  color: var(--river-green);
  filter: drop-shadow(0 1px 2px rgb(0 0 0 / 35%));
}

.edit-activity-log-section {
  padding-bottom: 0;
}

.edit-activity-log-hint {
  margin-left: auto;
  color: var(--color-text-muted);
  font-size: 11px;
  font-weight: 400;
}

.edit-activity-log-section .log-time .anticon {
  margin-right: 4px;
}

.edit-activity-log-section .log-entry.processing .log-time {
  color: var(--color-primary);
}

.edit-activity-log-section .log-entry.processing .ant-spin-dot-item {
  background-color: var(--color-primary);
}

.article-editor-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.ai-editor-section {
  gap: 12px;
}

.ai-editor-section .panel-title {
  margin-bottom: 0;
}

.ai-editor-section > * + * {
  margin-top: 12px;
}

.ai-edit-mode {
  display: flex;
  width: 100%;
}

.ai-edit-mode :deep(.ant-radio-button-wrapper) {
  flex: 1;
  text-align: center;
}

.ai-edit-image-select {
  width: 100%;
}

.ai-editor-hint {
  color: var(--color-text-secondary);
  font-size: 12px;
  line-height: 1.6;
}

.inline-ai-editor {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 20px;
  border: 2px dashed rgba(46, 104, 91, 0.82);
  border-radius: var(--radius-lg);
  background:
    linear-gradient(90deg, rgba(237, 244, 232, 0.9), rgba(255, 255, 255, 0.74)),
    rgba(236, 243, 229, 0.62);
}

.inline-ai-editor-title {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #2e685b;
  font-size: 18px;
  font-weight: 700;
}

.inline-ai-editor-title .anticon {
  font-size: 22px;
}

.inline-ai-editor-select {
  width: min(100%, 360px);
}

.inline-ai-editor-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 180px;
  gap: 20px;
  align-items: start;
}

.inline-ai-editor-body :deep(.ant-input-textarea) {
  min-width: 0;
}

.inline-ai-editor-submit {
  min-height: 56px;
  border: 0;
  border-radius: var(--radius-md);
  background: #2e685b;
  box-shadow: none;
  font-size: 15px;
  font-weight: 600;
}

.inline-ai-editor-submit:not(:disabled):hover {
  background: #24564b;
}

@media (max-width: 768px) {
  .inline-ai-editor {
    padding: 16px;
  }

  .inline-ai-editor-title {
    font-size: 16px;
  }

  .inline-ai-editor-body {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .inline-ai-editor-submit {
    width: 100%;
  }

}

.markdown-body {
  line-height: 1.8;
  font-size: 15px;
  color: var(--color-text);

  :deep(h2) {
    font-size: 20px;
    font-weight: 600;
    margin: 24px 0 14px;
    padding-bottom: 10px;
    border-bottom: 1px solid var(--color-border);
    color: var(--color-text);
  }

  :deep(p) {
    margin-bottom: 14px;
    text-indent: 2em;
  }

  :deep(img) {
    display: block;
    max-width: 100%;
    max-height: 600px;
    width: auto;
    height: auto;
    margin: 20px auto;
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-md);
    object-fit: contain;
  }

  // Mermaid 图表特殊处理（SVG 格式）
  :deep(img[src$='.svg']) {
    max-width: 800px;
    max-height: 500px;
  }
}

.typing-cursor {
  display: inline-block;
  animation: blink 1s infinite;
  color: var(--color-primary);
  font-weight: bold;
  font-size: 18px;
}

.image-progress-box {
  background: var(--color-background-secondary);
  border-radius: var(--radius-lg);
  padding: 24px;
  margin-top: 24px;
  text-align: center;

  .progress-header {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    margin-bottom: 16px;
    font-size: 15px;
    font-weight: 600;
    color: var(--color-text);
  }

  .progress-hint {
    margin: 12px 0 0;
    font-size: 13px;
    color: var(--color-text-muted);
  }
}

.loading-placeholder {
  text-align: center;
  padding: 100px 0;

  p {
    margin: 16px 0 0;
    color: var(--color-text-secondary);
    font-size: 15px;
  }
}

/* 完成状态 */
.success-header {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: var(--gradient-primary);
  border-radius: var(--radius-full);
  margin-bottom: 24px;
  color: white;
  font-size: 14px;
  font-weight: 600;

  .success-icon {
    font-size: 16px;
  }
}

/* 右侧辅助面板 */
.sidebar-right {
  background: white;
  border-left: 1px solid var(--color-border);
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-width: 0;
  overflow: visible;
}

.panel-section {
  padding-bottom: 20px;
  border-bottom: 1px solid var(--color-border-light);

  &:last-of-type {
    border-bottom: none;
    padding-bottom: 0;
  }
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 16px;
}

/* 配额信息样式 */
.quota-section {
  background: linear-gradient(135deg, rgba(34, 197, 94, 0.05) 0%, rgba(34, 197, 94, 0.02) 100%);
  border-radius: var(--radius-lg);
  padding: 16px !important;
  margin: -8px -8px 12px -8px;
}

.quota-admin {
  display: flex;
  align-items: center;
  gap: 10px;
}

.quota-badge {
  padding: 4px 10px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 600;

  &.admin {
    background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
    color: white;
  }

  &.vip {
    background: var(--gradient-primary);
    color: white;
  }
}

.quota-text {
  font-size: 14px;
  color: var(--color-text-secondary);
}

.quota-info {
  text-align: center;
}

.quota-display {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 4px;
}

.quota-number {
  font-size: 36px;
  font-weight: 700;
  color: var(--color-primary);
  line-height: 1;

  &.low {
    color: #faad14;
  }

  &.empty {
    color: #ff4d4f;
  }
}

.quota-unit {
  font-size: 14px;
  color: var(--color-text-muted);
}

.quota-label {
  font-size: 12px;
  color: var(--color-text-muted);
  margin: 4px 0 12px;
}

.quota-progress {
  max-width: 120px;
  margin: 0 auto;
}

/* 热门选题 */
.hot-topics-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 26px;
  margin: 2px 0 8px;
}

.hot-topics-meta {
  color: var(--color-text-muted);
  font-size: 11px;
}

.hot-topics-refresh {
  height: 24px;
  padding: 0 6px;
  color: var(--color-primary);
  font-size: 12px;
}

.hot-topics-refresh:hover {
  color: var(--color-primary-hover);
  background: rgba(34, 197, 94, 0.08);
}

.hot-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.hot-tag {
  display: inline-block;
  padding: 8px 12px;
  background: var(--color-background-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 12px;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);

  &:hover {
    border-color: var(--color-primary);
    color: var(--color-primary);
    background: rgba(34, 197, 94, 0.05);
    transform: translateY(-1px);
  }
}

/* 创作技巧 */
.tips-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tip-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  background: var(--color-background-secondary);
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);

  &:hover {
    background: rgba(34, 197, 94, 0.05);
  }
}

.tip-icon {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--gradient-primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.tip-content {
  flex: 1;
  min-width: 0;
}

.tip-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 2px;
}

.tip-desc {
  font-size: 11px;
  color: var(--color-text-muted);
  line-height: 1.4;
}

/* 创作进度信息 */
.progress-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.progress-step {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  background: var(--color-background-secondary);
  border-radius: var(--radius-md);
}

.step-label {
  font-size: 12px;
  color: var(--color-text-muted);
}

.step-value {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-primary);
}

.progress-tip {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 12px;
  background: rgba(34, 197, 94, 0.08);
  border-radius: var(--radius-md);
  font-size: 12px;
  color: var(--color-primary-dark);
  line-height: 1.5;

  .anticon {
    flex-shrink: 0;
    margin-top: 2px;
  }

  &.waiting {
    background: rgba(250, 173, 20, 0.08);
    color: #d48806;
  }
}

/* 实时日志 */
.realtime-logs-section {
  .logs-empty {
    padding: 12px 10px;
    border: 1px dashed var(--color-border-light);
    border-radius: var(--radius-md);
    color: var(--color-text-muted);
    font-size: 12px;
    text-align: center;
  }

  .logs-container {
    max-height: 300px;
    overflow-y: auto;
    background: var(--color-background);
    border-radius: var(--radius-md);
    border: 1px solid var(--color-border-light);
    padding: 8px;

    .log-entry {
      display: flex;
      gap: 8px;
      padding: 6px 8px;
      font-size: 11px;
      line-height: 1.4;
      border-radius: var(--radius-sm);
      margin-bottom: 4px;
      transition: background var(--transition-fast);

      &:hover {
        background: var(--color-background-secondary);
      }

      &.success {
        .log-time {
          color: var(--color-success);
        }
      }

      &.error {
        background: rgba(239, 68, 68, 0.05);
        .log-time {
          color: var(--color-error);
        }
        .log-message {
          color: var(--color-error);
        }
      }

      .log-time {
        flex-shrink: 0;
        color: var(--color-text-muted);
        font-weight: 500;
      }

      .log-message {
        flex: 1;
        color: var(--color-text-secondary);
      }
    }

    &::-webkit-scrollbar {
      width: 6px;
    }

    &::-webkit-scrollbar-thumb {
      background: var(--color-border);
      border-radius: var(--radius-full);
    }

    &::-webkit-scrollbar-track {
      background: transparent;
    }
  }
}

/* 选题展示 */
.topic-display {
  padding: 12px 16px;
  background: var(--color-background-secondary);
  border-radius: var(--radius-md);
  border-left: 3px solid var(--color-primary);

  p {
    margin: 0;
    font-size: 13px;
    color: var(--color-text);
    line-height: 1.6;
  }
}

/* 提示面板样式 */
.tips-section {
  .tip-icon {
    background: transparent;
    font-size: 16px;
  }

  .tip-desc {
    font-size: 12px;
  }
}

/* 文章统计 */
.stats-section {
  margin-top: 16px;
  width: 100%;
  min-width: 0;
  box-sizing: border-box;
  padding: 14px 12px 16px;
  background: rgba(247, 245, 238, 0.76);
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-md);

  .panel-title {
    margin-bottom: 12px;
  }
}

.sidebar-right .stats-section:last-of-type {
  padding-bottom: 16px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.stat-item {
  text-align: center;
  min-width: 0;
  padding: 12px 8px;
  background: rgba(255, 255, 255, 0.68);
  border: 1px solid rgba(143, 184, 164, 0.22);
  border-radius: var(--radius-md);
}

.stat-value {
  min-width: 0;
  font-size: clamp(18px, 2vw, 22px);
  line-height: 1.2;
  font-weight: 700;
  color: var(--color-primary);
  margin-bottom: 3px;
  overflow-wrap: anywhere;
}

.stat-label {
  font-size: 12px;
  color: var(--color-text-muted);
}

/* 底部帮助链接 */
.panel-footer {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--color-border-light);
  display: flex;
  justify-content: center;
  gap: 20px;
}

.help-link {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
  font-size: 12px;
  color: var(--color-text);
  cursor: pointer;
  transition: color var(--transition-fast);

  &:hover {
    color: var(--color-primary);
  }
}

.help-steps {
  display: grid;
  gap: 16px;
  margin: 0;
  padding-left: 24px;
  color: var(--color-text-secondary);
  line-height: 1.7;

  strong {
    color: var(--color-text);
  }
}

.feedback-upload-trigger {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 7px 12px;
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-primary);
  cursor: pointer;

  input {
    display: none;
  }
}

.feedback-image-list {
  display: grid;
  gap: 8px;
  margin-top: 12px;
}

.feedback-image-item {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  padding: 8px 10px;
  border-radius: var(--radius-md);
  background: rgba(34, 197, 94, 0.06);
  color: var(--color-text-secondary);

  span {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.action-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.action-btn {
  height: 40px;
  font-size: 13px;
  font-weight: 500;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;

  &.primary {
    background: var(--gradient-primary);
    border: none;
    color: white;

    &:hover {
      opacity: 0.9;
    }
  }
}

/* 阶段切换过渡动画 */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.3s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateX(30px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateX(-30px);
}

/* 动画 */
@keyframes blink {
  0%,
  50% {
    opacity: 1;
  }
  51%,
  100% {
    opacity: 0;
  }
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@keyframes pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

/* 加载阶段样式 */
.loading-stage {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 20px;
  width: min(100%, 560px);
  min-height: 420px;
  margin: 0 auto;
  padding: 56px 40px;
  text-align: center;

  h3 {
    font-size: 20px;
    font-weight: 600;
    color: var(--color-text);
    margin: 0 0 8px;
  }

  p {
    font-size: 14px;
    color: var(--color-text-secondary);
    margin: 0;
  }
}

.loading-stage-icon {
  display: grid;
  place-items: center;
  width: 52px;
  height: 52px;
  border: 1px solid rgba(69, 111, 100, 0.2);
  border-radius: 50%;
  background: rgba(143, 184, 164, 0.12);
  color: var(--color-primary);
  font-size: 22px;
  animation: loading-breathe 1.8s ease-in-out infinite;
}

.loading-stage-copy {
  display: grid;
  gap: 6px;
}

.loading-eyebrow {
  color: var(--color-primary);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.loading-stage > .animated-skeleton {
  width: min(100%, 360px);
  margin-top: 8px;
}

/* 大纲生成中状态 */
.outline-generating-state {
  max-width: 100%;
}

.outline-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 40px 20px;
  font-size: 14px;
  color: var(--color-text-secondary);
}

.outline-loading .animated-skeleton {
  width: min(100%, 420px);
  margin-top: 4px;
}

.content-loading {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 16px;
  min-height: 260px;
  margin-top: 24px;
  padding: 28px;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-xl);
  background: rgba(247, 250, 246, 0.72);
  color: var(--color-text-secondary);
}

.content-loading p {
  margin: 4px 0 0;
  line-height: 1.7;
}

.content-loading .animated-skeleton {
  grid-column: 1 / -1;
  width: 100%;
}

.stage-enter-active,
.stage-leave-active {
  transition: opacity 180ms ease, transform 220ms var(--ease-out);
}

.stage-enter-from {
  opacity: 0;
  transform: translateY(12px);
}

.stage-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

@keyframes loading-breathe {
  0%,
  100% {
    box-shadow: 0 0 0 0 rgba(143, 184, 164, 0.12);
  }
  50% {
    box-shadow: 0 0 0 10px rgba(143, 184, 164, 0.02);
  }
}

/* 渐入动画 */
@keyframes fade-in {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.fade-in {
  animation: fade-in 0.4s ease-out;
}

/* 响应式 */
@media (max-width: 1400px) {
  .create-layout {
    grid-template-columns: 280px 1fr 260px;
  }
}

@media (max-width: 1200px) {
  .create-layout {
    grid-template-columns: 240px 1fr 220px;
  }
}

@media (max-width: 992px) {
  .article-create-page {
    height: auto;
    min-height: calc(100vh - 64px);
    overflow: visible;
  }

  .create-layout {
    grid-template-columns: 1fr;
    height: auto;
  }

  .sidebar-left,
  .sidebar-right {
    display: none;
  }

  .main-content {
    padding: 20px;
  }
}

/* 沅水青山创作工作台 */
.article-create-page {
  min-height: 0;
  position: relative;
  isolation: isolate;
  background:
    radial-gradient(circle at 65% 2%, rgba(143, 184, 164, 0.18), transparent 38rem),
    var(--paper-warm);
}

.create-layout {
  min-height: 0;
  position: relative;
  z-index: 1;
  background: transparent;
}

.sidebar-left,
.sidebar-right {
  background: rgba(247, 245, 238, 0.58);
}

.sidebar-left {
  background: linear-gradient(90deg, rgba(247, 245, 238, 0.58), rgba(247, 245, 238, 0.34));
  border-right-color: var(--line-soft);
}

.sidebar-right {
  border-left-color: var(--line-soft);
}

.sidebar-header {
  border-bottom-color: var(--line-soft);
}

.sidebar-title {
  color: var(--ink-deep);
  font-weight: 600;
}

.flow-item {
  padding: 17px 0;
}

.flow-item:not(:last-child)::before {
  background: var(--line-soft);
}

.flow-item.completed::before {
  background: var(--river-green);
}

.flow-item.active::before {
  background: linear-gradient(180deg, var(--mountain-green) 52%, var(--line-soft) 52%);
}

.flow-item.active .flow-indicator {
  border-color: var(--mountain-green);
  background: rgba(143, 184, 164, 0.18);
  color: var(--mountain-green);
  box-shadow: 0 0 0 6px rgba(143, 184, 164, 0.1);
}

.flow-item.completed .flow-indicator {
  background: var(--mountain-green);
}

.main-content {
  background:
    linear-gradient(rgba(247, 245, 238, 0.3), rgba(247, 245, 238, 0.38)),
    repeating-linear-gradient(0deg, transparent, transparent 31px, rgba(69, 111, 100, 0.035) 32px);
}

.input-state {
  max-width: 760px;
  padding-top: clamp(28px, 8vh, 96px);
}

.input-card {
  overflow: hidden;
  border: 1px solid rgba(69, 111, 100, 0.14);
  border-radius: var(--radius-xl);
  background: rgba(255, 255, 255, 0.72);
  box-shadow: var(--shadow-xl);
}

.input-card::before {
  display: none;
}

.input-header {
  margin-bottom: 28px;
}

.input-title {
  color: var(--ink-deep);
  font-size: clamp(2rem, 4vw, 3rem);
  font-weight: 500;
}

.input-subtitle {
  color: var(--ink-muted);
}

.topic-textarea {
  border-color: var(--line-soft);
  background: rgba(247, 245, 238, 0.76);
}

.style-section,
.image-methods-section,
.outline-preview,
.image-progress-box {
  border-color: var(--line-soft);
  background: rgba(243, 247, 243, 0.7);
}

.style-group :deep(.ant-radio-wrapper),
.methods-group :deep(.ant-checkbox-wrapper) {
  border-color: var(--line-soft);
  background: rgba(255, 255, 255, 0.7);
}

.style-group :deep(.ant-radio-wrapper-checked),
.methods-group :deep(.ant-checkbox-wrapper-checked) {
  border-color: var(--mountain-green);
  background: rgba(143, 184, 164, 0.18);
}

.create-btn.ant-btn {
  box-shadow: var(--shadow-green) !important;
}

.panel-section {
  border-bottom-color: var(--line-soft);
}

.quota-section {
  background: linear-gradient(135deg, rgba(143, 184, 164, 0.22), rgba(247, 245, 238, 0.4));
}

.hot-tag,
.tip-item,
.progress-step,
.topic-display {
  border-color: var(--line-soft);
  background: rgba(255, 255, 255, 0.52);
}

.hot-tag:hover,
.tip-item:hover {
  border-color: var(--river-green);
  background: rgba(143, 184, 164, 0.15);
}

.preview-header {
  border-bottom-color: var(--line-soft);
}

.article-title {
  color: var(--ink-deep);
  font-family: var(--font-display);
  font-size: clamp(2rem, 4vw, 3.4rem);
  font-weight: 500;
}

.article-title-input,
.article-subtitle-input {
  display: block;
  width: min(100%, 760px);
  margin-right: auto;
  margin-left: auto;
  border-color: transparent;
  background: rgba(255, 255, 255, 0.7);
  text-align: center;

  &:focus,
  &:hover {
    border-color: var(--river-green);
  }
}

.article-title-input {
  height: auto;
  margin-bottom: 12px;
  padding: 8px 16px;
  color: var(--ink-deep);
  font-family: var(--font-display);
  font-size: clamp(2rem, 4vw, 3.4rem);
  font-weight: 500;
  line-height: 1.4;
}

.article-subtitle-input {
  padding: 6px 12px;
  color: var(--color-text-secondary);
  font-size: 16px;
}

.outline-item {
  border-left-color: var(--river-green);
  background: rgba(255, 255, 255, 0.74);
  animation: stream-section-in 420ms var(--ease-out) both;
}

.content-preview {
  padding: clamp(24px, 5vw, 58px);
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-xl);
  background: rgba(255, 255, 255, 0.76);
  box-shadow: var(--shadow-card);
}

.article-reading-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: clamp(22px, 3vw, 32px);
  padding-bottom: 16px;
  border-bottom: 1px solid var(--line-soft);
}

.article-reading-label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--river-green);
  font-size: 17px;
  font-weight: 700;

  .anticon {
    font-size: 18px;
  }
}

.article-reading-note {
  color: var(--color-text-muted);
  font-size: 13px;
}

.completed-state .markdown-body {
  max-width: 50rem;
  margin: 0 auto;
  font-size: 16px;
  line-height: 2.05;

  :deep(p) {
    margin: 0 0 1.35em;
  }

  :deep(h2) {
    margin: 2.4em 0 1em;
    color: var(--ink-deep);
  }

  :deep(ul),
  :deep(ol) {
    margin: 0 0 1.35em;
    padding-left: 1.6em;
  }
}

.success-header {
  background: var(--gradient-primary);
  box-shadow: var(--shadow-green);
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition:
    opacity 420ms var(--ease-out),
    transform 420ms var(--ease-out),
    filter 420ms var(--ease-out);
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(18px);
  filter: blur(4px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-10px);
  filter: blur(3px);
}

.loading-stage,
.outline-generating-state {
  animation: stage-settle 520ms var(--ease-out) both;
}

@keyframes stream-section-in {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes stage-settle {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
/* 临水书桌背景：保留中间工作台阅读区域 */
.article-create-page::before {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 0;
  background:
    linear-gradient(
      90deg,
      rgba(230, 242, 235, 0.12) 0%,
      rgba(247, 245, 238, 0.04) 52%,
      rgba(247, 245, 238, 0) 100%
    ),
    url('@/assets/scenes/create-desk-layout.webp');
  background-blend-mode: normal;
  background-position: 88% center;
  background-size: cover;
  background-attachment: fixed;
  filter: saturate(1.16) contrast(1.12);
  pointer-events: none;
}

.creative-pulse {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-width: 92px;
  height: 26px;
  margin-bottom: 12px;
  padding: 0 10px;
  border: 1px solid rgba(69, 111, 100, 0.14);
  border-radius: var(--radius-full);
  background: rgba(143, 184, 164, 0.12);
  color: var(--river-green);
}

.creative-pulse > .anticon {
  font-size: 13px;
  animation: creative-spark 2.4s ease-in-out infinite;
}

.creative-pulse span {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--river-green);
  animation: creative-dot 1.5s ease-in-out infinite;
}

.creative-pulse span:nth-of-type(2) {
  animation-delay: 160ms;
}

.creative-pulse span:nth-of-type(3) {
  animation-delay: 320ms;
}

.input-card {
  padding: clamp(22px, 2.4vw, 32px);
}

.input-state {
  padding-top: clamp(18px, 4vh, 42px);
}

.input-header {
  margin-bottom: 18px;
}

.input-title {
  font-size: clamp(2rem, 3.1vw, 2.55rem);
}

.input-area {
  gap: 12px;
}

.reference-upload,
.style-section,
.image-methods-section {
  padding: 12px;
}

.section-header {
  margin-bottom: 8px;
}

.hot-topics-viewport {
  height: 248px;
  overflow: hidden;
}

.hot-topics-viewport .hot-tags {
  position: relative;
}

.hot-topic-drop-move,
.hot-topic-drop-enter-active {
  transition:
    transform 460ms var(--ease-out),
    opacity 460ms var(--ease-out);
}

.hot-topic-drop-enter-from {
  opacity: 0;
  transform: translateY(-30px);
}

.hot-topic-drop-leave-active {
  position: absolute;
  opacity: 0;
  transition: none;
}

.tips-list {
  gap: 8px;
}

.tip-item {
  gap: 10px;
  padding: 10px;
}

.tip-desc {
  line-height: 1.5;
}

@keyframes creative-dot {
  0%,
  100% {
    opacity: 0.35;
    transform: scale(0.8);
  }
  50% {
    opacity: 1;
    transform: scale(1.18);
  }
}

@keyframes creative-spark {
  0%,
  100% {
    transform: translateY(1px);
  }
  50% {
    transform: translateY(-2px);
  }
}

@media (max-height: 900px) and (min-width: 993px) {
  .main-content {
    padding: 16px 28px;
  }

  .input-state {
    padding-top: 8px;
  }

  .input-card {
    padding: 18px 24px;
  }

  .creative-pulse {
    height: 22px;
    margin-bottom: 8px;
  }

  .input-header {
    margin-bottom: 12px;
  }

  .input-subtitle {
    font-size: 14px;
  }

  .topic-textarea :deep(textarea.ant-input) {
    min-height: 112px !important;
  }

  .reference-upload,
  .style-section,
  .image-methods-section {
    padding: 10px 12px;
  }

  .sidebar-right {
    gap: 14px;
    padding: 18px;
  }

  .panel-title {
    margin-bottom: 10px;
  }
}

@media (max-width: 992px) {
  .sidebar-left.has-article-summary {
    display: block;
    padding: 16px 20px 0;
    border-right: 0;
    background: transparent;
  }

  .sidebar-left.has-article-summary .sidebar-header,
  .sidebar-left.has-article-summary .flow-timeline {
    display: none;
  }

  .sidebar-left.has-article-summary .article-summary-panel {
    max-width: 760px;
    margin: 0 auto;
  }
}

@media (prefers-reduced-motion: reduce) {
  .fade-slide-enter-active,
  .fade-slide-leave-active {
    transition: none;
  }

  .creative-pulse > .anticon,
  .creative-pulse span {
    animation: none;
  }

  .hot-topic-drop-move,
  .hot-topic-drop-enter-active,
  .hot-topic-drop-leave-active {
    transition: none;
  }
}

.create-layout,
.sidebar-left,
.main-content,
.sidebar-right,
.flow-item,
.panel-section,
.input-state,
.input-card {
  will-change: transform, opacity;
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition:
    opacity 720ms var(--ease-out),
    transform 720ms var(--ease-out),
    clip-path 720ms var(--ease-out);
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translate3d(0, 42px, 0) scale(0.97);
  clip-path: inset(0 0 16% 0);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translate3d(0, -18px, 0) scale(0.985);
  clip-path: inset(12% 0 0 0);
}

@media (max-width: 768px) {
  .reference-upload-header,
  .reference-summary-meta {
    align-items: flex-start;
    flex-wrap: wrap;
  }

  .reference-upload-header > div:first-child {
    flex: 1 1 100%;
  }

  .article-create-page::before {
    background-attachment: scroll;
    background-position: 68% center;
  }
}

@media (prefers-reduced-motion: reduce) {
  .fade-slide-enter-active,
  .fade-slide-leave-active {
    transition: none;
  }

  .create-layout,
  .sidebar-left,
  .main-content,
  .sidebar-right,
  .flow-item,
  .panel-section,
  .input-state,
  .input-card {
    will-change: auto;
  }
}
</style>
