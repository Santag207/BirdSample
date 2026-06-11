import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:url_launcher/url_launcher.dart';
import '../../providers/bird_provider.dart';
import '../../models/models.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> with SingleTickerProviderStateMixin {
  late TabController _tabController;
  final TextEditingController _searchController = TextEditingController();

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 3, vsync: this);
  }

  @override
  void dispose() {
    _tabController.dispose();
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<BirdProvider>();
    final theme = Theme.of(context);

    return Scaffold(
      body: CustomScrollView(
        slivers: [
          SliverAppBar(
            floating: true,
            title: const Text('OrnithoCache Pro', style: TextStyle(fontWeight: FontWeight.bold)),
            backgroundColor: theme.colorScheme.surface,
          ),
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.all(16.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  _buildHeroCard(context),
                  const SizedBox(height: 24),
                  Text('Manual del Operador Científico',
                    style: theme.textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.bold,
                      color: theme.colorScheme.primary
                    )
                  ),
                  const SizedBox(height: 12),
                  TabBar(
                    controller: _tabController,
                    labelColor: theme.colorScheme.primary,
                    unselectedLabelColor: theme.colorScheme.onSurfaceVariant,
                    indicatorColor: theme.colorScheme.primary,
                    tabs: const [
                      Tab(text: 'Guía de Uso'),
                      Tab(text: 'Funciones'),
                      Tab(text: 'Ideas'),
                    ],
                  ),
                  const SizedBox(height: 12),
                  SizedBox(
                    height: 200,
                    child: TabBarView(
                      controller: _tabController,
                      children: [
                        _buildGuideTab(),
                        _buildFunctionsTab(),
                        _buildIdeasTab(),
                      ],
                    ),
                  ),
                  const SizedBox(height: 24),
                  _buildAiSearchCard(context, provider),
                  const SizedBox(height: 24),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text('Noticias de Conservación',
                            style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold, color: theme.colorScheme.primary)
                          ),
                          Text('Feed oficial de allaboutbirds.org', style: theme.textTheme.bodySmall),
                        ],
                      ),
                      IconButton(
                        onPressed: provider.isRefreshingArticles ? null : () => provider.fetchArticlesFromAllAboutBirds(),
                        icon: provider.isRefreshingArticles
                          ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2))
                          : Icon(Icons.refresh, color: theme.colorScheme.primary),
                      )
                    ],
                  ),
                  const SizedBox(height: 12),
                ],
              ),
            ),
          ),
          SliverList(
            delegate: SliverChildBuilderDelegate(
              (context, index) {
                if (provider.birdArticles.isEmpty) {
                  return const Padding(
                    padding: EdgeInsets.all(32.0),
                    child: Center(child: Text('Cargando noticias...', style: TextStyle(color: Colors.grey))),
                  );
                }
                return _buildNewsItem(context, provider.birdArticles[index]);
              },
              childCount: provider.birdArticles.isEmpty ? 1 : provider.birdArticles.length,
            ),
          ),
          const SliverToBoxAdapter(child: SizedBox(height: 32)),
        ],
      ),
    );
  }

  Widget _buildHeroCard(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      elevation: 0,
      color: theme.colorScheme.primaryContainer.withOpacity(0.7),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: Padding(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          children: [
            Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(8),
                  decoration: BoxDecoration(
                    color: theme.colorScheme.primary,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: const Icon(Icons.eco, color: Colors.white, size: 28),
                ),
                const SizedBox(width: 14),
                const Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('OrnithoCache Pro', style: TextStyle(fontWeight: FontWeight.w900, fontSize: 20)),
                    Text('Monitoreo offline & clasificador', style: TextStyle(fontSize: 12)),
                  ],
                )
              ],
            ),
            const SizedBox(height: 16),
            const Text(
              'Bienvenido a la plataforma líder para biólogos. Registra campañas ecológicas en campo de forma 100% offline y sincronízalas automáticamente.',
              style: TextStyle(fontSize: 13, height: 1.4),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildGuideTab() {
    return const SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _ManualItem(title: 'Paso 1: Crear el Proyecto', desc: 'Accede a Muestreos y crea un proyecto indicando el contrato.'),
          _ManualItem(title: 'Paso 2: Registrar Sitios', desc: 'Añade puntos georreferenciados con coordenadas GPS.'),
          _ManualItem(title: 'Paso 3: Campañas y Plantillas', desc: 'Selecciona un método de muestreo y completa los campos.'),
          _ManualItem(title: 'Paso 4: Ingresar Avistamientos', desc: 'Registra cantidad, comportamiento y evidencias.'),
        ],
      ),
    );
  }

  Widget _buildFunctionsTab() {
    return const SingleChildScrollView(
      child: Column(
        children: [
          _FeatureItem(icon: Icons.cloud_off, title: 'Caché Offline', desc: 'Todo se guarda localmente en SQLite.'),
          _FeatureItem(icon: Icons.mic, title: 'Biometría Acústica', desc: 'Registra vocalizaciones directamente.'),
          _FeatureItem(icon: Icons.picture_as_pdf, title: 'Reportes PDF', desc: 'Genera informes listos para exportar.'),
        ],
      ),
    );
  }

  Widget _buildIdeasTab() {
    return const SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _ManualItem(title: '💡 Impacto Forestal', desc: 'Monitoreo antes y después de intervenciones.'),
          _ManualItem(title: '🔍 Inventarios Comunitarios', desc: 'Documenta especies polinizadoras con la comunidad.'),
          _ManualItem(title: '🦜 Seguimiento IUCN', desc: 'Focaliza en especies amenazadas como el Cóndor Andino.'),
        ],
      ),
    );
  }

  Widget _buildAiSearchCard(BuildContext context, BirdProvider provider) {
    final theme = Theme.of(context);
    return Card(
      elevation: 0,
      color: theme.colorScheme.primary.withOpacity(0.05),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: BorderSide(color: theme.colorScheme.primary.withOpacity(0.15)),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Clasificación Científica IA',
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14, color: theme.colorScheme.primary)
            ),
            const Text('Ingresa rasgos físicos para buscar en All About Birds.',
              style: TextStyle(fontSize: 11, color: Colors.grey)
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _searchController,
              style: const TextStyle(fontSize: 13),
              decoration: InputDecoration(
                hintText: 'Ej: Colibrí con plumas brillantes...',
                isDense: true,
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(8)),
                suffixIcon: IconButton(
                  icon: const Icon(Icons.clear, size: 16),
                  onPressed: () => _searchController.clear(),
                ),
              ),
            ),
            const SizedBox(height: 10),
            Align(
              alignment: Alignment.centerRight,
              child: FilledButton.icon(
                onPressed: provider.aiLoading ? null : () {
                  if (_searchController.text.isNotEmpty) {
                    provider.searchBirdWithGemini(_searchController.text, "", "");
                  }
                },
                icon: provider.aiLoading
                  ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                  : const Icon(Icons.search, size: 16),
                label: const Text('Consultar IA', style: TextStyle(fontSize: 12)),
              ),
            ),
            if (provider.aiResponse.isNotEmpty) ...[
              const SizedBox(height: 12),
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: Colors.grey.shade300),
                ),
                child: Text(provider.aiResponse, style: const TextStyle(fontSize: 12)),
              )
            ]
          ],
        ),
      ),
    );
  }

  Widget _buildNewsItem(BuildContext context, BirdArticle article) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 4.0),
      child: Card(
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(12),
          side: BorderSide(color: Colors.black.withOpacity(0.06)),
        ),
        child: InkWell(
          onTap: () => launchUrl(Uri.parse(article.link)),
          borderRadius: BorderRadius.circular(12),
          child: Padding(
            padding: const EdgeInsets.all(14.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                      decoration: BoxDecoration(
                        color: Theme.of(context).colorScheme.primaryContainer,
                        borderRadius: BorderRadius.circular(6),
                      ),
                      child: Text(article.category, style: const TextStyle(fontSize: 9, fontWeight: FontWeight.bold)),
                    ),
                    Text(article.pubDate.substring(0, article.pubDate.length > 16 ? 16 : article.pubDate.length),
                      style: const TextStyle(fontSize: 9, color: Colors.grey)
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                Text(article.title, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13), maxLines: 2, overflow: TextOverflow.ellipsis),
                const SizedBox(height: 4),
                Text(article.description, style: const TextStyle(fontSize: 11, color: Colors.black87), maxLines: 3, overflow: TextOverflow.ellipsis),
                const SizedBox(height: 8),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text('Autor: ${article.creator}', style: const TextStyle(fontSize: 9, fontStyle: FontStyle.italic, color: Colors.grey)),
                    const Row(
                      children: [
                        Text('Leer más', style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: Colors.green)),
                        Icon(Icons.arrow_outward, size: 10, color: Colors.green),
                      ],
                    )
                  ],
                )
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _ManualItem extends StatelessWidget {
  final String title;
  final String desc;
  const _ManualItem({required this.title, required this.desc});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 12, color: Colors.green)),
          Text(desc, style: const TextStyle(fontSize: 11, color: Colors.grey)),
        ],
      ),
    );
  }
}

class _FeatureItem extends StatelessWidget {
  final IconData icon;
  final String title;
  final String desc;
  const _FeatureItem({required this.icon, required this.title, required this.desc});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4.0),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, size: 18, color: Colors.green),
          const SizedBox(width: 8),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 12)),
                Text(desc, style: const TextStyle(fontSize: 11, color: Colors.grey)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
