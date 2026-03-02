<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  createResume,
  deleteResume,
  fetchResumeDetail,
  fetchStudentResumes,
  updateResume,
  type ResumeDetail,
  type ResumeExperienceDetail,
  type ResumeSummary,
} from '../../api/student'

type ResumeExperienceForm = {
  title: string
  organization: string
  startDate: string
  endDate: string
  description: string
}

type ResumeSkillForm = {
  skill: string
  proficiency: number
}

const route = useRoute()
const router = useRouter()

const studentIdInput = ref('')
const studentIdError = ref('')
const studentId = ref<number | null>(null)

const resumes = ref<ResumeSummary[]>([])
const listLoading = ref(false)
const listError = ref('')
const deletingId = ref<number | null>(null)

const resumeForm = reactive({
  id: null as number | null,
  title: '',
  summary: '',
  portfolioUrl: '',
  experiences: [] as ResumeExperienceForm[],
  skills: [] as ResumeSkillForm[],
})

const resumeFormLoading = ref(false)
const resumeSaving = ref(false)
const resumeMessage = ref('')
const resumeError = ref('')

const showCreateModal = ref(false)
const createModal = reactive({ title: '', summary: '', portfolioUrl: '' })

const formatDate = (value: string | null | undefined) => {
  if (!value) return '—'
  try {
    return new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(value))
  } catch {
    return value
  }
}

const formatPeriod = (start: string, end: string) => {
  if (!start && !end) return '—'
  const n = (v: string) => (v ? v.replace(/-/g, '.') : '')
  if (start && end) return `${n(start)} — ${n(end)}`
  if (start) return `${n(start)} 起`
  return `至 ${n(end)}`
}

const parseStudentId = (value: string) => {
  const num = parseInt(value?.trim() ?? '', 10)
  return Number.isFinite(num) && num > 0 ? num : null
}

const createEmptyExperience = (): ResumeExperienceForm => ({
  title: '', organization: '', startDate: '', endDate: '', description: '',
})

const createEmptySkill = (): ResumeSkillForm => ({ skill: '', proficiency: 60 })

const replaceArray = <T>(target: T[], source: T[]) => {
  target.splice(0, target.length, ...source)
}

const fillFormFromDetail = (detail: ResumeDetail) => {
  resumeForm.id = detail.resume?.id ?? null
  resumeForm.title = detail.resume?.title ?? ''
  resumeForm.summary = detail.resume?.summary ?? ''
  resumeForm.portfolioUrl = detail.resume?.portfolioUrl ?? ''
  const experiences: ResumeExperienceForm[] = (detail.experiences ?? []).map((e: ResumeExperienceDetail) => ({
    title: e.title ?? '', organization: e.organization ?? '', startDate: e.startDate ?? '', endDate: e.endDate ?? '', description: e.description ?? '',
  }))
  const skills: ResumeSkillForm[] = (detail.skills ?? []).map((s) => ({
    skill: s.skill ?? '', proficiency: typeof s.proficiency === 'number' ? s.proficiency : 60,
  }))
  replaceArray(resumeForm.experiences, experiences.length ? experiences : [createEmptyExperience()])
  replaceArray(resumeForm.skills, skills.length ? skills : [createEmptySkill()])
}

const resetForm = () => {
  resumeForm.id = null
  resumeForm.title = ''
  resumeForm.summary = ''
  resumeForm.portfolioUrl = ''
  replaceArray(resumeForm.experiences, [createEmptyExperience()])
  replaceArray(resumeForm.skills, [createEmptySkill()])
  resumeMessage.value = ''
  resumeError.value = ''
}

const loadResumes = async (id: number, preferredId?: number | null) => {
  listLoading.value = true
  listError.value = ''
  try {
    const data = await fetchStudentResumes(id)
    resumes.value = data
    if (!data.length) {
      resetForm()
      return
    }
    const targetId = preferredId ?? resumeForm.id ?? data[0]?.id
    if (targetId) await editResume(targetId, { silent: true })
  } catch (e) {
    listError.value = e instanceof Error ? e.message : '加载失败'
    resumes.value = []
  } finally {
    listLoading.value = false
  }
}

const handleSelectStudent = async () => {
  const id = parseStudentId(studentIdInput.value)
  if (!id) {
    studentIdError.value = '请输入有效学生 ID'
    return
  }
  studentIdError.value = ''
  studentId.value = id
  localStorage.setItem('currentStudentId', String(id))
  await loadResumes(id)
  router.replace({ name: 'student-resume', query: { studentId: String(id) } })
}

const openCreateModal = () => {
  if (!studentId.value) {
    resumeError.value = '请先输入学生 ID'
    return
  }
  createModal.title = ''
  createModal.summary = ''
  createModal.portfolioUrl = ''
  showCreateModal.value = true
}

const closeCreateModal = () => { showCreateModal.value = false }

const confirmCreateResume = () => {
  if (!createModal.title.trim()) {
    alert('请填写简历标题')
    return
  }
  resetForm()
  resumeForm.title = createModal.title.trim()
  resumeForm.summary = createModal.summary.trim()
  resumeForm.portfolioUrl = createModal.portfolioUrl.trim()
  showCreateModal.value = false
  resumeMessage.value = '已创建草稿，请继续填写'
}

const addExperience = () => { resumeForm.experiences.push(createEmptyExperience()) }
const removeExperience = (i: number) => {
  resumeForm.experiences.splice(i, 1)
  if (!resumeForm.experiences.length) resumeForm.experiences.push(createEmptyExperience())
}
const addSkill = () => { resumeForm.skills.push(createEmptySkill()) }
const removeSkill = (i: number) => {
  resumeForm.skills.splice(i, 1)
  if (!resumeForm.skills.length) resumeForm.skills.push(createEmptySkill())
}

const editResume = async (id: number, opts: { silent?: boolean } = {}) => {
  if (!opts.silent) resumeFormLoading.value = true
  resumeError.value = ''
  try {
    const detail = await fetchResumeDetail(id)
    fillFormFromDetail(detail)
    if (!opts.silent) resumeMessage.value = '已载入'
  } catch (e) {
    resumeError.value = e instanceof Error ? e.message : '加载详情失败'
  } finally {
    if (!opts.silent) resumeFormLoading.value = false
  }
}

const removeResume = async (id: number) => {
  if (!studentId.value || !confirm('确定删除？不可恢复。')) return
  deletingId.value = id
  resumeError.value = ''
  resumeMessage.value = ''
  try {
    await deleteResume(id)
    if (resumeForm.id === id) resetForm()
    resumeMessage.value = '已删除'
    await loadResumes(studentId.value)
  } catch (e) {
    resumeError.value = e instanceof Error ? e.message : '删除失败'
  } finally {
    deletingId.value = null
  }
}

const submitResume = async () => {
  if (!studentId.value) {
    resumeError.value = '请先输入学生 ID'
    return
  }
  if (!resumeForm.title.trim()) {
    resumeError.value = '请填写简历标题'
    return
  }
  resumeSaving.value = true
  resumeError.value = ''
  resumeMessage.value = ''
  const payload = {
    studentId: studentId.value,
    title: resumeForm.title.trim(),
    summary: resumeForm.summary.trim(),
    portfolioUrl: resumeForm.portfolioUrl.trim(),
    experiences: resumeForm.experiences
      .filter((e) => e.title.trim())
      .map((e) => ({
        title: e.title.trim(),
        organization: e.organization.trim(),
        startDate: e.startDate || null,
        endDate: e.endDate || null,
        description: e.description.trim(),
      })),
    skills: resumeForm.skills
      .filter((s) => s.skill.trim())
      .map((s) => ({
        skill: s.skill.trim(),
        proficiency: Number.isFinite(s.proficiency) ? s.proficiency : null,
      })),
  }
  try {
    const detail = resumeForm.id
      ? await updateResume(resumeForm.id, payload)
      : await createResume(payload)
    fillFormFromDetail(detail)
    resumeMessage.value = resumeForm.id ? '已保存' : '已创建'
    await loadResumes(studentId.value, detail.resume.id)
  } catch (e) {
    resumeError.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    resumeSaving.value = false
  }
}

const previewExperiences = computed(() =>
  resumeForm.experiences
    .filter((e) => e.title.trim())
    .map((e) => ({
      ...e,
      period: formatPeriod(e.startDate, e.endDate),
      description: e.description.trim() || '暂无描述',
    }))
)
const previewSkills = computed(() =>
  resumeForm.skills
    .filter((s) => s.skill.trim())
    .map((s) => ({ ...s, proficiency: Math.min(100, Math.max(0, Number(s.proficiency) || 0)) }))
)
const previewTitle = computed(() => resumeForm.title.trim() || '未命名简历')
const previewSummary = computed(() => resumeForm.summary.trim() || '请填写摘要与求职意向。')
const previewPortfolio = computed(() => resumeForm.portfolioUrl.trim())

watch(
  () => route.query.studentId,
  async (val) => {
    const id = typeof val === 'string' ? parseStudentId(val) : null
    if (id && id !== studentId.value) {
      studentIdInput.value = String(id)
      studentId.value = id
      localStorage.setItem('currentStudentId', String(id))
      await loadResumes(id)
    }
    if (!id && val !== undefined) {
      studentId.value = null
      studentIdInput.value = ''
      resumes.value = []
      resetForm()
    }
  },
  { immediate: false }
)

onMounted(async () => {
  let userStudentId: number | null = null
  try {
    const raw = localStorage.getItem('userInfo')
    if (raw) {
      const info = JSON.parse(raw)
      if (info?.role === 'STUDENT' && info?.id) {
        userStudentId = info.id
        localStorage.setItem('currentStudentId', String(info.id))
      }
    }
  } catch (_) {}
  const queryId = typeof route.query.studentId === 'string' ? parseStudentId(route.query.studentId) : null
  const cached = parseStudentId(localStorage.getItem('currentStudentId') || '')
  const initial = queryId ?? userStudentId ?? cached
  if (initial) {
    studentId.value = initial
    studentIdInput.value = String(initial)
    await loadResumes(initial)
  } else {
    resetForm()
  }
})
</script>

<template>
  <div class="resume-page">
    <header class="page-header">
      <div class="page-header__title">
        <h1>简历管理</h1>
        <p>创建与编辑多份简历，用于不同求职场景。</p>
      </div>
      <div class="page-header__student">
        <label for="student-id">学生 ID</label>
        <div class="student-input-row">
          <input
            id="student-id"
            v-model="studentIdInput"
            type="text"
            inputmode="numeric"
            placeholder="输入 ID 后载入"
            @keyup.enter="handleSelectStudent"
          />
          <button type="button" class="btn btn-primary" @click="handleSelectStudent">载入</button>
        </div>
        <span v-if="studentIdError" class="text-error">{{ studentIdError }}</span>
      </div>
    </header>

    <div class="resume-layout">
      <aside class="sidebar">
        <div class="sidebar__top">
          <h2>我的简历</h2>
          <button type="button" class="btn btn-ghost" @click="openCreateModal">+ 新建</button>
        </div>
        <div v-if="listLoading" class="sidebar__state">加载中…</div>
        <div v-else-if="listError" class="sidebar__state sidebar__state--error">{{ listError }}</div>
        <div v-else-if="!resumes.length" class="sidebar__state sidebar__state--empty">
          <p>暂无简历</p>
          <button type="button" class="btn btn-text" @click="openCreateModal">创建第一份</button>
        </div>
        <ul v-else class="resume-list">
          <li
            v-for="r in resumes"
            :key="r.id"
            :class="['resume-item', { 'resume-item--active': resumeForm.id === r.id }]"
          >
            <button type="button" class="resume-item__main" @click="editResume(r.id)">
              <span class="resume-item__title">{{ r.title }}</span>
              <span class="resume-item__meta">{{ formatDate(r.updatedAt) }} · {{ r.experienceCount }} 经历 · {{ r.skillCount }} 技能</span>
            </button>
            <div class="resume-item__actions">
              <button type="button" class="btn btn-text" @click="editResume(r.id)">编辑</button>
              <button
                type="button"
                class="btn btn-text btn-danger"
                :disabled="deletingId === r.id"
                @click="removeResume(r.id)"
              >
                {{ deletingId === r.id ? '删除中' : '删除' }}
              </button>
            </div>
          </li>
        </ul>
      </aside>

      <main class="editor-pane">
        <div class="editor-card">
          <div class="editor-card__head">
            <h2>{{ resumeForm.id ? '编辑简历' : '新建简历' }}</h2>
            <button
              type="button"
              class="btn btn-primary"
              :disabled="resumeSaving"
              @click="submitResume"
            >
              {{ resumeSaving ? '保存中…' : resumeForm.id ? '保存' : '创建' }}
            </button>
          </div>
          <div v-if="resumeFormLoading" class="editor-card__loading">加载详情中…</div>
          <form v-else class="editor-form" @submit.prevent="submitResume">
            <div class="form-row form-row--2">
              <label>
                <span class="label">简历标题</span>
                <input v-model="resumeForm.title" type="text" placeholder="例如：2024 校招通用" />
              </label>
              <label>
                <span class="label">作品集链接</span>
                <input v-model="resumeForm.portfolioUrl" type="url" placeholder="可选" />
              </label>
            </div>
            <label class="form-full">
              <span class="label">摘要</span>
              <textarea v-model="resumeForm.summary" rows="3" placeholder="核心技能与求职意向" />
            </label>

            <section class="form-section">
              <div class="form-section__head">
                <h3>经历</h3>
                <button type="button" class="btn btn-text" @click="addExperience">+ 添加</button>
              </div>
              <div v-for="(exp, i) in resumeForm.experiences" :key="i" class="block">
                <div class="form-row form-row--2">
                  <label><span class="label">标题</span><input v-model="exp.title" type="text" placeholder="岗位/项目" /></label>
                  <label><span class="label">组织</span><input v-model="exp.organization" type="text" placeholder="公司/学校" /></label>
                </div>
                <div class="form-row form-row--2">
                  <label><span class="label">开始</span><input v-model="exp.startDate" type="date" /></label>
                  <label><span class="label">结束</span><input v-model="exp.endDate" type="date" /></label>
                </div>
                <label class="form-full"><span class="label">描述</span><textarea v-model="exp.description" rows="2" placeholder="职责与成果" /></label>
                <div class="block__actions"><button type="button" class="btn btn-text" @click="removeExperience(i)">移除</button></div>
              </div>
            </section>

            <section class="form-section">
              <div class="form-section__head">
                <h3>技能</h3>
                <button type="button" class="btn btn-text" @click="addSkill">+ 添加</button>
              </div>
              <div v-for="(skill, i) in resumeForm.skills" :key="i" class="skill-row">
                <input v-model="skill.skill" type="text" placeholder="技能名称" />
                <div class="skill-row__slider">
                  <input v-model.number="skill.proficiency" type="range" min="0" max="100" step="5" />
                  <span>{{ skill.proficiency }}%</span>
                </div>
                <button type="button" class="btn btn-text" @click="removeSkill(i)">删除</button>
              </div>
            </section>

            <div v-if="resumeMessage || resumeError" class="form-feedback">
              <span v-if="resumeMessage" class="text-success">{{ resumeMessage }}</span>
              <span v-if="resumeError" class="text-error">{{ resumeError }}</span>
            </div>
          </form>
        </div>
      </main>

      <aside class="preview-pane">
        <div class="preview-card">
          <h3>预览</h3>
          <div class="preview-paper">
            <div class="preview__header">
              <div class="preview__avatar">{{ previewTitle.charAt(0) || '?' }}</div>
              <div>
                <div class="preview__title">{{ previewTitle }}</div>
                <p class="preview__summary">{{ previewSummary }}</p>
                <a v-if="previewPortfolio" :href="previewPortfolio" target="_blank" rel="noopener" class="preview__link">作品集</a>
              </div>
            </div>
            <section class="preview__section">
              <h4>经历</h4>
              <template v-if="previewExperiences.length">
                <div v-for="(exp, i) in previewExperiences" :key="i" class="preview-exp">
                  <div class="preview-exp__time">{{ exp.period }}</div>
                  <div class="preview-exp__body">
                    <strong>{{ exp.title }}</strong>
                    <span>{{ exp.organization || '—' }}</span>
                    <p>{{ exp.description }}</p>
                  </div>
                </div>
              </template>
              <p v-else class="preview__empty">暂无经历</p>
            </section>
            <section class="preview__section">
              <h4>技能</h4>
              <template v-if="previewSkills.length">
                <div v-for="(s, i) in previewSkills" :key="i" class="preview-skill">
                  <span>{{ s.skill }}</span>
                  <div class="preview-skill__bar"><div class="preview-skill__fill" :style="{ width: s.proficiency + '%' }" /></div>
                  <span class="preview-skill__pct">{{ s.proficiency }}%</span>
                </div>
              </template>
              <p v-else class="preview__empty">暂无技能</p>
            </section>
          </div>
        </div>
      </aside>
    </div>

    <div v-if="showCreateModal" class="modal-overlay" @click.self="closeCreateModal">
      <div class="modal">
        <div class="modal__head">
          <h2>新建简历</h2>
          <button type="button" class="modal__close" aria-label="关闭" @click="closeCreateModal">×</button>
        </div>
        <div class="modal__body">
          <label class="field">
            <span class="field__label">标题 <em>*</em></span>
            <input v-model="createModal.title" type="text" placeholder="例如：2024 校招通用" />
          </label>
          <label class="field">
            <span class="field__label">摘要</span>
            <textarea v-model="createModal.summary" rows="3" placeholder="可选" />
          </label>
          <label class="field">
            <span class="field__label">作品集链接</span>
            <input v-model="createModal.portfolioUrl" type="url" placeholder="可选" />
          </label>
        </div>
        <div class="modal__foot">
          <button type="button" class="btn btn-secondary" @click="closeCreateModal">取消</button>
          <button type="button" class="btn btn-primary" @click="confirmCreateResume">创建</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.resume-page {
  min-height: 100vh;
  background: #f5f5f7;
  padding: 32px 40px 48px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

.page-header {
  max-width: 1200px;
  margin: 0 auto 32px;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 24px;
}

.page-header__title h1 {
  font-size: 28px;
  font-weight: 600;
  color: #1d1d1f;
  margin: 0 0 4px;
  letter-spacing: -0.02em;
}

.page-header__title p {
  font-size: 15px;
  color: #86868b;
  margin: 0;
}

.page-header__student {
  flex-shrink: 0;
}

.page-header__student label {
  display: block;
  font-size: 12px;
  font-weight: 500;
  color: #86868b;
  margin-bottom: 6px;
}

.student-input-row {
  display: flex;
  gap: 8px;
}

.student-input-row input {
  width: 140px;
  padding: 10px 12px;
  border: 1px solid #d2d2d7;
  border-radius: 10px;
  font-size: 14px;
  background: #fff;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.student-input-row input:focus {
  outline: none;
  border-color: #0071e3;
  box-shadow: 0 0 0 4px rgba(0, 113, 227, 0.15);
}

.resume-layout {
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 280px 1fr 360px;
  gap: 24px;
  align-items: start;
}

.sidebar {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 24px;
}

.sidebar__top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.sidebar__top h2 {
  font-size: 17px;
  font-weight: 600;
  color: #1d1d1f;
  margin: 0;
}

.sidebar__state {
  padding: 24px 16px;
  text-align: center;
  font-size: 14px;
  color: #86868b;
  border-radius: 10px;
  background: #f5f5f7;
}

.sidebar__state--error {
  color: #bf4800;
  background: #fff4e5;
}

.sidebar__state--empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.sidebar__state--empty p {
  margin: 0;
}

.resume-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.resume-item {
  border-radius: 10px;
  border: 1px solid transparent;
  background: #f5f5f7;
  transition: background 0.2s, border-color 0.2s;
}

.resume-item--active {
  background: #fff;
  border-color: #0071e3;
  box-shadow: 0 0 0 1px #0071e3;
}

.resume-item__main {
  width: 100%;
  padding: 14px 16px;
  text-align: left;
  border: none;
  background: none;
  cursor: pointer;
  display: block;
}

.resume-item__title {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #1d1d1f;
  margin-bottom: 4px;
}

.resume-item__meta {
  font-size: 12px;
  color: #86868b;
}

.resume-item__actions {
  display: flex;
  gap: 8px;
  padding: 0 16px 12px;
}

.editor-pane {
  min-width: 0;
}

.editor-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.editor-card__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid #e8e8ed;
}

.editor-card__head h2 {
  font-size: 20px;
  font-weight: 600;
  color: #1d1d1f;
  margin: 0;
}

.editor-card__loading {
  padding: 32px;
  text-align: center;
  color: #86868b;
  font-size: 14px;
}

.editor-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-row {
  display: grid;
  gap: 16px;
}

.form-row--2 {
  grid-template-columns: 1fr 1fr;
}

.form-full {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.label {
  font-size: 13px;
  font-weight: 500;
  color: #1d1d1f;
  display: block;
  margin-bottom: 4px;
}

.editor-form input,
.editor-form textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d2d2d7;
  border-radius: 10px;
  font-size: 14px;
  font-family: inherit;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.editor-form input:focus,
.editor-form textarea:focus {
  outline: none;
  border-color: #0071e3;
  box-shadow: 0 0 0 4px rgba(0, 113, 227, 0.15);
}

.editor-form textarea {
  resize: vertical;
  min-height: 72px;
}

.form-section {
  padding: 20px;
  background: #f5f5f7;
  border-radius: 12px;
}

.form-section__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.form-section__head h3 {
  font-size: 15px;
  font-weight: 600;
  color: #1d1d1f;
  margin: 0;
}

.block {
  padding: 16px;
  background: #fff;
  border-radius: 10px;
  border: 1px solid #e8e8ed;
  margin-bottom: 12px;
}

.block:last-child {
  margin-bottom: 0;
}

.block .form-row,
.block .form-full {
  margin-bottom: 12px;
}

.block .form-row:last-child,
.block .form-full:last-child {
  margin-bottom: 0;
}

.block__actions {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #e8e8ed;
}

.skill-row {
  display: grid;
  grid-template-columns: 1fr auto auto;
  gap: 12px;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #e8e8ed;
}

.skill-row:last-child {
  border-bottom: none;
}

.skill-row input[type='text'] {
  padding: 8px 10px;
  border: 1px solid #d2d2d7;
  border-radius: 8px;
  font-size: 14px;
}

.skill-row__slider {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 140px;
}

.skill-row__slider input[type='range'] {
  flex: 1;
  accent-color: #0071e3;
}

.skill-row__slider span {
  font-size: 13px;
  font-weight: 500;
  color: #0071e3;
  width: 36px;
  text-align: right;
}

.form-feedback {
  padding: 12px 16px;
  border-radius: 10px;
  background: #f5f5f7;
  font-size: 14px;
}

.text-success { color: #34c759; }
.text-error { color: #bf4800; }

.btn {
  padding: 10px 18px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  transition: opacity 0.2s, transform 0.1s;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-primary {
  background: #0071e3;
  color: #fff;
}

.btn-primary:hover:not(:disabled) {
  opacity: 0.9;
}

.btn-secondary {
  background: #e8e8ed;
  color: #1d1d1f;
}

.btn-secondary:hover {
  background: #d2d2d7;
}

.btn-ghost {
  background: transparent;
  color: #0071e3;
}

.btn-ghost:hover {
  text-decoration: underline;
}

.btn-text {
  background: transparent;
  color: #0071e3;
  padding: 6px 10px;
}

.btn-text:hover {
  text-decoration: underline;
}

.btn-danger {
  color: #bf4800;
}

.preview-pane {
  position: sticky;
  top: 24px;
}

.preview-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.preview-card h3 {
  font-size: 15px;
  font-weight: 600;
  color: #1d1d1f;
  margin: 0 0 16px;
}

.preview-paper {
  background: #fbfbfd;
  border-radius: 10px;
  padding: 20px;
  border: 1px solid #e8e8ed;
  font-size: 13px;
}

.preview__header {
  display: flex;
  gap: 14px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e8e8ed;
}

.preview__avatar {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: #0071e3;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 600;
  flex-shrink: 0;
}

.preview__title {
  font-size: 16px;
  font-weight: 600;
  color: #1d1d1f;
  margin-bottom: 4px;
}

.preview__summary {
  color: #86868b;
  margin: 0 0 6px;
  line-height: 1.5;
}

.preview__link {
  font-size: 13px;
  color: #0071e3;
  text-decoration: none;
}

.preview__link:hover {
  text-decoration: underline;
}

.preview__section {
  margin-bottom: 18px;
}

.preview__section:last-child {
  margin-bottom: 0;
}

.preview__section h4 {
  font-size: 12px;
  font-weight: 600;
  color: #86868b;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  margin: 0 0 10px;
}

.preview__empty {
  color: #86868b;
  margin: 0;
  font-size: 13px;
}

.preview-exp {
  display: grid;
  grid-template-columns: 90px 1fr;
  gap: 12px;
  margin-bottom: 14px;
}

.preview-exp:last-child {
  margin-bottom: 0;
}

.preview-exp__time {
  font-size: 12px;
  color: #86868b;
  font-weight: 500;
}

.preview-exp__body {
  padding: 10px 12px;
  background: #fff;
  border-radius: 8px;
  border-left: 3px solid #0071e3;
}

.preview-exp__body strong {
  display: block;
  color: #1d1d1f;
  margin-bottom: 2px;
}

.preview-exp__body span {
  font-size: 12px;
  color: #86868b;
  margin-bottom: 6px;
  display: block;
}

.preview-exp__body p {
  margin: 0;
  color: #515154;
  font-size: 12px;
  line-height: 1.5;
}

.preview-skill {
  display: grid;
  grid-template-columns: 1fr 80px 32px;
  gap: 10px;
  align-items: center;
  margin-bottom: 10px;
}

.preview-skill:last-child {
  margin-bottom: 0;
}

.preview-skill__bar {
  height: 6px;
  background: #e8e8ed;
  border-radius: 3px;
  overflow: hidden;
}

.preview-skill__fill {
  height: 100%;
  background: #0071e3;
  border-radius: 3px;
  transition: width 0.2s;
}

.preview-skill__pct {
  font-size: 12px;
  font-weight: 600;
  color: #0071e3;
  text-align: right;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(8px);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  animation: fade 0.25s ease;
}

@keyframes fade {
  from { opacity: 0; }
  to { opacity: 1; }
}

.modal {
  background: #fff;
  border-radius: 16px;
  width: 100%;
  max-width: 440px;
  box-shadow: 0 24px 48px rgba(0, 0, 0, 0.18);
  overflow: hidden;
  animation: slide 0.3s ease;
}

@keyframes slide {
  from {
    opacity: 0;
    transform: scale(0.96) translateY(10px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.modal__head {
  padding: 20px 24px;
  border-bottom: 1px solid #e8e8ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal__head h2 {
  font-size: 20px;
  font-weight: 600;
  color: #1d1d1f;
  margin: 0;
}

.modal__close {
  width: 32px;
  height: 32px;
  border: none;
  background: #f5f5f7;
  border-radius: 8px;
  font-size: 22px;
  line-height: 1;
  color: #86868b;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s, color 0.2s;
}

.modal__close:hover {
  background: #e8e8ed;
  color: #1d1d1f;
}

.modal__body {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field__label {
  font-size: 13px;
  font-weight: 500;
  color: #1d1d1f;
}

.field__label em {
  color: #bf4800;
  font-style: normal;
}

.field input,
.field textarea {
  padding: 10px 12px;
  border: 1px solid #d2d2d7;
  border-radius: 10px;
  font-size: 14px;
  font-family: inherit;
}

.field input:focus,
.field textarea:focus {
  outline: none;
  border-color: #0071e3;
  box-shadow: 0 0 0 4px rgba(0, 113, 227, 0.15);
}

.modal__foot {
  padding: 16px 24px 24px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 1024px) {
  .resume-layout {
    grid-template-columns: 260px 1fr;
  }

  .preview-pane {
    grid-column: 1 / -1;
    position: static;
  }
}

@media (max-width: 768px) {
  .resume-page {
    padding: 20px 16px 32px;
  }

  .page-header {
    flex-direction: column;
    align-items: stretch;
    margin-bottom: 24px;
  }

  .resume-layout {
    grid-template-columns: 1fr;
    gap: 20px;
  }

  .sidebar {
    position: static;
  }

  .form-row--2 {
    grid-template-columns: 1fr;
  }

  .editor-card__head {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }

  .skill-row {
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .preview-exp {
    grid-template-columns: 1fr;
  }

  .preview-skill {
    grid-template-columns: 1fr auto;
  }

  .preview-skill__bar {
    grid-column: 1 / -1;
  }
}
</style>
