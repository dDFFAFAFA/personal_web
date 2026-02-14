import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import DefaultLayout from '../layouts/DefaultLayout.vue'
import Home from '../pages/home/index.vue'
import PaperList from '../pages/papers/index.vue'
import PaperDetail from '../pages/papers/detail.vue'
import NoteEdit from '../pages/papers/note-edit.vue'
import Tags from '../pages/tags/index.vue'
import Settings from '../pages/settings/index.vue'

const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    component: DefaultLayout,
    children: [
      {
        path: '',
        name: 'Home',
        component: Home
      },
      {
        path: 'papers',
        name: 'Papers',
        component: PaperList
      },
      {
        path: 'papers/:id',
        name: 'PaperDetail',
        component: PaperDetail
      },
      {
        path: 'papers/:id/notes/:noteId',
        name: 'NoteEdit',
        component: NoteEdit
      },
      {
        path: 'tags',
        name: 'Tags',
        component: Tags
      },
      {
        path: 'settings',
        name: 'Settings',
        component: Settings
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
