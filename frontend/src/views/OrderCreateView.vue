<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppLayout from '../components/AppLayout.vue'
import { fetchCustomers } from '../api/customers'
import { fetchCustomerPets } from '../api/pets'
import { fetchEnabledServiceItems } from '../api/serviceItems'
import { createOrder } from '../api/orders'

const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const formRef = ref()
const customers = ref([])
const pets = ref([])
const serviceItems = ref([])

const form = reactive({
  customerId: '',
  petId: '',
  serviceItemIds: [],
  appointmentTime: '',
  remark: '',
})

const rules = {
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  petId: [{ required: true, message: '请选择宠物', trigger: 'change' }],
  serviceItemIds: [{ required: true, type: 'array', min: 1, message: '请选择服务项目', trigger: 'change' }],
  appointmentTime: [{ required: true, message: '请选择预约时间', trigger: 'change' }],
}

const customerOptions = computed(() => customers.value.map((item) => ({
  label: `${item.name} / ${item.phone}`,
  value: item.id,
})))

const totalAmount = computed(() => serviceItems.value
  .filter((item) => form.serviceItemIds.includes(item.id))
  .reduce((sum, item) => sum + Number(item.price), 0))

watch(() => form.customerId, async (customerId) => {
  form.petId = ''
  pets.value = []
  if (customerId) {
    const result = await fetchCustomerPets(customerId, true)
    pets.value = result.records
  }
})

function formatDateTime(value) {
  if (!value) {
    return ''
  }
  const date = new Date(value)
  const pad = (number) => String(number).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

async function loadBaseData() {
  loading.value = true
  try {
    const [customerResult, serviceItemResult] = await Promise.all([
      fetchCustomers({ status: 'ENABLED', page: 1, pageSize: 100 }),
      fetchEnabledServiceItems(),
    ])
    customers.value = customerResult.records
    serviceItems.value = serviceItemResult.records
  } finally {
    loading.value = false
  }
}

async function submitOrder() {
  await formRef.value.validate()
  saving.value = true
  try {
    const result = await createOrder({
      customerId: Number(form.customerId),
      petId: Number(form.petId),
      serviceItemIds: form.serviceItemIds.map(Number),
      appointmentTime: formatDateTime(form.appointmentTime),
      remark: form.remark,
    })
    ElMessage.success('订单已创建')
    router.push(`/orders/${result.order.id}`)
  } finally {
    saving.value = false
  }
}

onMounted(loadBaseData)
</script>

<template>
  <AppLayout>
    <div class="page-header">
      <h1 class="page-title">新建订单</h1>
      <el-button @click="router.push('/orders')">返回列表</el-button>
    </div>

    <el-card v-loading="loading" shadow="never" class="form-card">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="客户" prop="customerId">
          <el-select v-model="form.customerId" filterable placeholder="请选择客户" class="wide-control">
            <el-option v-for="item in customerOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="宠物" prop="petId">
          <el-select v-model="form.petId" filterable placeholder="请先选择客户" class="wide-control">
            <el-option v-for="item in pets" :key="item.id" :label="`${item.name} / ${item.species}`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="服务项目" prop="serviceItemIds">
          <el-checkbox-group v-model="form.serviceItemIds" class="service-options">
            <el-checkbox v-for="item in serviceItems" :key="item.id" :value="item.id" border>
              {{ item.name }} / ￥{{ Number(item.price).toFixed(2) }} / {{ item.durationMinutes }}分钟
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="预约时间" prop="appointmentTime">
          <el-date-picker
            v-model="form.appointmentTime"
            type="datetime"
            placeholder="请选择预约时间"
            format="YYYY-MM-DD HH:mm:ss"
            class="wide-control"
          />
        </el-form-item>
        <el-form-item label="订单金额">
          <strong class="amount">￥{{ totalAmount.toFixed(2) }}</strong>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="submitOrder">创建订单</el-button>
          <el-button @click="router.push('/orders')">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </AppLayout>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.form-card {
  max-width: 900px;
  border-radius: 8px;
}

.wide-control {
  width: 360px;
}

.service-options {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.amount {
  font-size: 20px;
  color: #1f2937;
}

</style>
