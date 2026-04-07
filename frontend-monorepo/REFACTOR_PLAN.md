# MultiModuleApp 重构计划

## 目标
- 对齐 Angular v21+ 最佳实践：全面采用 standalone、signals、`input()`/`output()`、`inject()`。
- 提升可维护性：解耦页面状态、统一类型、减少副作用与重复逻辑。
- 提升可访问性：保证关键页面通过 WCAG AA 与 AXE 检查。
- 提升性能：路由懒加载、组件按需加载、减少不必要变更检测与渲染。

## 现状摘要（基于当前代码）
1. 架构处于“NgModule + standalone 混用”阶段，`AppModule`、`DataQueryModule`、`ExternalIframeModule` 仍是模块式组织。  
2. 路由是集中式定义，尚未做 feature 级懒加载。  
3. 业务状态主要依赖 `BehaviorSubject` 与组件内部可变字段，尚未系统迁移为 signals。  
4. 模板仍大量使用 `*ngIf/*ngFor`，尚未迁移到控制流语法 `@if/@for`。  
5. 类型约束存在提升空间（如 `any[]`、`EventEmitter<any>`）。  
6. 可访问性与自动化质量门槛（AXE/Lighthouse/WCAG）尚未纳入持续检查。

## 分阶段实施

### Phase 0：基线与防回归（1~2 天）
- 增加“重构基线”文档与验收清单（路由行为、工作区 tab 同步、登录跳转、数据查询主路径）。
- 补充关键行为测试：
  - `WorkspaceComponent` 路由 query 与 tab 状态互相恢复。
  - `AuthGuard` 未登录跳转、已登录放行。
  - `ConfigService` 外部应用缓存与失败重试行为。
- 引入基础可访问性检查（先在 CI 里作为非阻断警告，第二阶段转阻断）。

**交付物**
- 核心流程测试用例集。
- 重构验收表（功能、性能、可访问性）。

---

### Phase 1：应用骨架现代化（2~4 天）
- 从 `AppModule` 迁移到 `bootstrapApplication` + `app.config.ts`。
- 将路由迁移为 `provideRouter`，并改造为 feature 级懒加载：
  - `home`、`workspace`、`auth`、`explorer` 按 feature 切分 route config。
- 清理模块化残留：移除 `DataQueryModule`/`ExternalIframeModule`，改为 standalone 组件导出。

**验收标准**
- 构建与启动正常。
- 主导航路由行为与现有一致。
- 首屏 JS 包体和路由分块可见（至少 explorer/query 相关代码不进入首屏包）。

---

### Phase 2：状态管理迁移到 signals（3~5 天）
- `TabService`：
  - 用 `signal<TabItem[]>` 和 `signal<number>` 取代 `BehaviorSubject`。
  - 暴露 `computed` 的 `activeTabId`、`tabIds`，减少重复计算。
- `HomeComponent`：
  - `modules/favoriteIds/currentTab/searchTerm/isLoading` 转为 signals。
  - `displayedModules` 改为 `computed()`，保持纯函数过滤逻辑。
- `WorkspaceComponent`：
  - 精简订阅管理，优先使用路由 observable + `toSignal`（或统一 effect 层）。

**验收标准**
- 功能行为不变。
- 去除手动 `detectChanges()` 与可避免的订阅管理样板代码。

---

### Phase 3：组件 API 与类型收敛（3~4 天）
- 统一采用 `input()` / `output()`，替换 `@Input/@Output`。
- 把 `any` 相关类型收敛为明确接口：
  - 模块卡片类型、表格行模型、查询 payload/response、行点击事件类型。
- 服务依赖注入统一迁移到 `inject()`。
- 组件统一设置 `ChangeDetectionStrategy.OnPush`（已经是 OnPush 的保持）。

**验收标准**
- ESLint/TS 严格模式下零新增 `any`。
- 对外组件输入输出语义清晰，类型可追踪。

---

### Phase 4：模板与可访问性治理（2~4 天）
- 将 `*ngIf/*ngFor` 迁移为 `@if/@for`。
- 关键交互补齐可访问性：
  - 收藏/关闭/切换等点击元素补充语义与键盘可操作性。
  - 表单控件增加可关联 label、错误提示语义、焦点管理。
  - `mat-icon` 纯装饰图标加 `aria-hidden`，操作图标提供可读名称。
- 引入 AXE 自动化检查并作为 CI 阻断。

**验收标准**
- 首页、登录页、workspace 页、query 页通过 AXE。
- 核心交互符合 WCAG AA（键盘、焦点、名称/角色/值）。

---

### Phase 5：数据查询能力拆分与性能优化（3~5 天）
- 拆分 `DataQueryComponent`：
  - 查询条件构建（query form）
  - 查询执行与结果状态
  - 表格展示与列宽策略
  - 聚合对话框
- 对网格渲染和列宽策略做性能审视：避免重复自动 sizing，必要时仅首渲染执行。
- 统一错误处理与用户提示（替代仅 `console.error/warn`）。

**验收标准**
- `DataQueryComponent` 体量明显下降，职责单一。
- 查询、筛选、聚合与 tab 切换性能不回退。

## 风险与缓解
- **风险：路由迁移导致深链接回归**  
  缓解：先写路由恢复测试，再迁移实现；保留 `workspace/:moduleId` 兼容期。
- **风险：signals 迁移引入状态时序问题**  
  缓解：按服务逐个迁移，先 `TabService` 再页面；每步都跑回归测试。
- **风险：无障碍改造影响视觉一致性**  
  缓解：设计与前端联合验收，建立对照清单（视觉 + A11y 双签）。

## 里程碑建议
- M1（第 1 周）：Phase 0 + Phase 1 完成。
- M2（第 2 周）：Phase 2 + Phase 3 完成。
- M3（第 3 周）：Phase 4 + Phase 5 完成并稳定。

## 建议优先级（如果要先做最小可行重构）
1. 先做路由懒加载 + standalone 架构收口（收益最大、风险可控）。
2. 再做 `TabService` 和首页 signals 化（对业务体验直接）。
3. 随后做 A11y 阻断与模板控制流迁移。
4. 最后做 DataQuery 组件拆分与性能深优化。
