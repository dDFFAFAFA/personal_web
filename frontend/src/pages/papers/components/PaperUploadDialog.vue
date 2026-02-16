<template>
  <el-dialog
    v-model="visible"
    title="上传新论文"
    width="600px"
    destroy-on-close
    :close-on-click-modal="false"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" label-position="top">
      <el-form-item label="论文文件 (PDF)" prop="file">
        <el-upload
          class="upload-demo"
          drag
          action=""
          :auto-upload="false"
          :limit="1"
          accept=".pdf"
          :on-change="handleFileChange"
          :on-remove="handleFileRemove"
          :file-list="fileList"
        >
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">
            拖拽文件到此处或 <em>点击上传</em>
          </div>
        </el-upload>
      </el-form-item>

      <el-divider content-position="center">元数据信息</el-divider>

      <el-form-item label="DOI (智能填充)" prop="doi">
        <div style="display: flex; width: 100%; gap: 10px;">
          <el-input v-model="form.doi" placeholder="10.xxxx/xxxxx" clearable @clear="handleDoiClear" />
          <el-button type="primary" :loading="enrichLoading" @click="handleSmartFill" :icon="MagicStick">
            自动填充
          </el-button>
        </div>
      </el-form-item>

      <el-form-item label="标题" prop="title">
        <el-input v-model="form.title" placeholder="请输入论文标题" />
      </el-form-item>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="作者 (逗号分隔)" prop="authorsStr">
            <el-input v-model="form.authorsStr" placeholder="Author A, Author B" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="发表年份" prop="year">
            <el-input-number v-model="form.year" :min="1900" :max="new Date().getFullYear() + 1" style="width: 100%" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="会议/期刊" prop="venue">
            <el-input v-model="form.venue" placeholder="e.g. CVPR" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="CCF 等级">
            <el-select v-model="form.ccfRank" placeholder="选择等级" clearable>
              <el-option label="CCF-A" value="A" />
              <el-option label="CCF-B" value="B" />
              <el-option label="CCF-C" value="C" />
              <el-option label="None" value="N" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
           <el-form-item label="JCR 分区">
            <el-select v-model="form.jcrQuartile" placeholder="选择分区" clearable>
              <el-option label="Q1" value="Q1" />
              <el-option label="Q2" value="Q2" />
              <el-option label="Q3" value="Q3" />
              <el-option label="Q4" value="Q4" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="摘要" prop="abstractText">
        <el-input v-model="form.abstractText" type="textarea" rows="3" />
      </el-form-item>
    </el-form>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="submitUpload">
          上传
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { UploadFilled, MagicStick } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type UploadFile } from 'element-plus'
import { createPaper } from '../../../api/paper'
import { enrichByDoi, enrichByFile, enrichByTitle, lookupVenue } from '../../../api/metadata'
import type { MetadataEnrichResponse } from '../../../types/paper'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits(['update:modelValue', 'success'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const formRef = ref<FormInstance>()
const loading = ref(false)
const enrichLoading = ref(false)
const fileList = ref<UploadFile[]>([])

const form = reactive({
  title: '',
  authorsStr: '',
  year: new Date().getFullYear(),
  venue: '',
  doi: '',
  abstractText: '',
  ccfRank: '',
  jcrQuartile: ''
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  file: [{ required: true, message: '请上传PDF文件', trigger: 'change', validator: (_rule: any, _value: any, callback: any) => {
    if (fileList.value.length === 0) callback(new Error('请上传PDF文件'))
    else callback()
  }}]
}

const handleFileChange = (uploadFile: UploadFile) => {
  if (uploadFile.raw) {
    fileList.value = [uploadFile]
    // Auto-fill title from filename if empty
    if (!form.title && uploadFile.name) {
      form.title = uploadFile.name.replace('.pdf', '')
    }
  }
}

const handleFileRemove = () => {
  fileList.value = []
}

const handleDoiClear = () => {
  // Optional: clear other fields? No.
}

const handleSmartFill = async () => {
  enrichLoading.value = true
  try {
    let data: MetadataEnrichResponse | null = null
    if (form.doi) {
      const res = await enrichByDoi(form.doi)
      data = res.data
      if (isMetadataEmpty(data) && form.title) {
        const fallback = await enrichByTitle(form.title)
        data = fallback.data
      }
    } else if (fileList.value[0]?.raw) {
      const file = fileList.value[0].raw as File
      const fileRes = await enrichByFile(file, form.title)
      data = fileRes.data
      if (!hasSubstantialMetadata(data) && form.title) {
        const titleRes = await enrichByTitle(form.title)
        data = mergeMetadata(data, titleRes.data)
      }
    } else if (form.title) {
      const res = await enrichByTitle(form.title)
      data = res.data
    } else {
      ElMessage.warning('请填写 DOI 或标题以进行自动填充')
      return
    }

    if (!data || isMetadataEmpty(data)) {
      ElMessage.warning('未检索到可用元数据，请换 DOI 或手动输入标题后重试')
      return
    }

    form.title = data.title || form.title
    form.authorsStr = data.authors ? data.authors.join(', ') : form.authorsStr
    form.year = data.year || form.year
    form.venue = data.venue || form.venue
    form.doi = data.doi || form.doi
    form.abstractText = data.abstractText || form.abstractText
    form.ccfRank = data.ccfRank || form.ccfRank
    form.jcrQuartile = data.jcrQuartile || form.jcrQuartile

    if (form.venue && !form.ccfRank && !form.jcrQuartile) {
      try {
        const venueRes = await lookupVenue(form.venue)
        if (venueRes.data) {
          form.ccfRank = venueRes.data.ccfRank || form.ccfRank
          form.jcrQuartile = venueRes.data.jcrQuartile || form.jcrQuartile
        }
      } catch (_e) {
        // Ignore ranking lookup failures, it should not block basic metadata fill.
      }
    }
    if (hasSubstantialMetadata(data)) {
      ElMessage.success('元数据填充成功')
    } else {
      ElMessage.warning('仅识别到标题，未检索到作者/年份/刊物等信息')
    }
  } catch (error) {
    console.error(error)
  } finally {
    enrichLoading.value = false
  }
}

const isMetadataEmpty = (data?: MetadataEnrichResponse | null) => {
  if (!data) return true
  return !data.title
    && (!data.authors || data.authors.length === 0)
    && !data.year
    && !data.venue
    && !data.doi
    && !data.abstractText
}

const hasSubstantialMetadata = (data?: MetadataEnrichResponse | null) => {
  if (!data) return false
  return !!(
    (data.authors && data.authors.length > 0)
    || data.year
    || data.venue
    || data.doi
    || data.abstractText
    || data.ccfRank
    || data.jcrQuartile
  )
}

const mergeMetadata = (
  primary: MetadataEnrichResponse | null,
  secondary: MetadataEnrichResponse | null
): MetadataEnrichResponse | null => {
  if (!primary) return secondary
  if (!secondary) return primary
  return {
    title: primary.title || secondary.title,
    authors: (primary.authors && primary.authors.length > 0) ? primary.authors : secondary.authors,
    year: primary.year ?? secondary.year,
    venue: primary.venue || secondary.venue,
    doi: primary.doi || secondary.doi,
    abstractText: primary.abstractText || secondary.abstractText,
    paperUrl: primary.paperUrl || secondary.paperUrl,
    citationCount: primary.citationCount ?? secondary.citationCount,
    ccfRank: primary.ccfRank || secondary.ccfRank,
    jcrQuartile: primary.jcrQuartile || secondary.jcrQuartile,
    source: primary.source || secondary.source
  }
}

const submitUpload = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid && fileList.value.length > 0 && fileList.value[0]?.raw) {
      loading.value = true
      try {
        const formData = new FormData()
        formData.append('file', fileList.value[0]!.raw!)
        formData.append('title', form.title)
        
        const authors = form.authorsStr.split(/[,，]/).map(s => s.trim()).filter(Boolean)
        formData.append('authors', JSON.stringify(authors))
        
        formData.append('year', form.year.toString())
        if (form.venue) formData.append('venue', form.venue)
        if (form.doi) formData.append('doi', form.doi)
        if (form.abstractText) formData.append('abstractText', form.abstractText)
        if (form.ccfRank) formData.append('ccfRank', form.ccfRank)
        if (form.jcrQuartile) formData.append('jcrQuartile', form.jcrQuartile)

        const res = await createPaper(formData)
        if (res.code === 200) {
          ElMessage.success('上传成功')
          visible.value = false
          emit('success')
          // Reset
          formRef.value?.resetFields()
          fileList.value = []
          form.ccfRank = ''
          form.jcrQuartile = ''
        }
      } catch (error) {
        console.error(error)
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.upload-demo {
  width: 100%;
}
</style>
