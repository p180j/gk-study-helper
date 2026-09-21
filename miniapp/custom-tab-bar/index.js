Component({
  data: {
    selected: 0,
    tabs: [
      { pagePath: '/pages/home/home', icon: '今', text: '首页' },
      { pagePath: '/pages/learn/learn', icon: '学', text: '学习' },
      { pagePath: '/pages/ability/ability', icon: '能', text: '能力' }
    ]
  },
  lifetimes: {
    attached() { this.syncSelected() }
  },
  pageLifetimes: {
    show() { this.syncSelected() }
  },
  methods: {
    syncSelected() {
      const pages = getCurrentPages()
      const route = pages.length ? '/' + pages[pages.length - 1].route : ''
      const selected = this.data.tabs.findIndex(item => item.pagePath === route)
      if (selected >= 0 && selected !== this.data.selected) this.setData({ selected })
    },
    switchTab(event) {
      const index = Number(event.currentTarget.dataset.index)
      const tab = this.data.tabs[index]
      if (!tab || index === this.data.selected) return
      this.setData({ selected: index })
      wx.switchTab({ url: tab.pagePath })
    }
  }
})
