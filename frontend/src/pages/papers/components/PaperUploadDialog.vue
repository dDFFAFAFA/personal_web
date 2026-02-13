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
        >
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">
            拖拽文件到此处或 <em>点击上传</em>
          </div>
        </el-upload>
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
        <el-col :span="12">
          <el-form-item label="会议/期刊" prop="venue">
            <el-input v-model="form.venue" placeholder="e.g. CVPR" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="DOI" prop="doi">
            <el-input v-model="form.doi" placeholder="10.xxxx/..." />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="摘要" prop="abstractText">
        <el-input v-model="form.abstractText" type="textarea" rows="3" />
      </el-form-item>
      
      <!-- Tags selection could go here, omitting for brevity in Phase 1 start -->
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
import { UploadFilled } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type UploadFile } from 'element-plus'
import { createPaper } from '../../../api/paper'

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
const fileList = ref<File[]>([])

const form = reactive({
  title: '',
  authorsStr: '',
  year: new Date().getFullYear(),
  venue: '',
  doi: '',
  abstractText: ''
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
    fileList.value = [uploadFile.raw]
    // Auto-fill title from filename if empty
    if (!form.title && uploadFile.name) {
      form.title = uploadFile.name.replace('.pdf', '')
    }
  }
}

const handleFileRemove = () => {
  fileList.value = []
}

const submitUpload = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid && fileList.value[0]) {
      loading.value = true
      try {
        const formData = new FormData()
        formData.append('file', fileList.value[0])
        formData.append('title', form.title)
        
        const authors = form.authorsStr.split(/[,，]/).map(s => s.trim()).filter(Boolean)
        formData.append('authors', JSON.stringify(authors))
        
        formData.append('year', form.year.toString())
        if (form.venue) formData.append('venue', form.venue)
        if (form.doi) formData.append('doi', form.doi)
        if (form.abstractText) formData.append('abstractText', form.abstractText)

        const res = await createPaper(formData)
        if (res.code === 200) {
          ElMessage.success('上传成功')
          visible.value = false
          emit('success')
          // Reset
          formRef.value?.resetFields()
          fileList.value = []
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
