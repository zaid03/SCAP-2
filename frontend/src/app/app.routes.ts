import { RouterModule, Routes } from '@angular/router';
import { NgModule } from '@angular/core';
import { LoginComponent } from './login/login.component';
import { EntComponent } from './ent/ent.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { CentrogestorComponent } from './centrogestor/centrogestor.component';
import { EjeComponent } from './eje/eje.component';
import { ConsultaAnaliticaAlmacenesComponent } from './consulta-analitica-almacenes/consulta-analitica-almacenes.component';
import { ConsultaAnalticaDeArticulosComponent } from './consulta-analtica-de-articulos/consulta-analtica-de-articulos.component';
import { ConsultaAnalticaDeFamiliasComponent } from './consulta-analtica-de-familias/consulta-analtica-de-familias.component';
import { ConsultaArticulosAlmacenComponent } from './consulta-articulos-almacen/consulta-articulos-almacen.component';
import { ConsultaPendienteContabilizarComponent } from './consulta-pendiente-contabilizar/consulta-pendiente-contabilizar.component';

export const routes: Routes = [
    { path: 'login', component: LoginComponent },
    { path: 'ent', component: EntComponent },
    { path: 'dashboard', component: DashboardComponent },
    { path: 'eje', component: EjeComponent},
    { path: 'centro-gestor', component: CentrogestorComponent},
    { path: 'almacenes', component: ConsultaAnaliticaAlmacenesComponent},
    { path: 'Carticulos', component: ConsultaAnalticaDeArticulosComponent},
    { path: 'Cfamilia', component: ConsultaAnalticaDeFamiliasComponent},
    { path: 'CAlmacen', component:ConsultaArticulosAlmacenComponent},
    { path: 'Ccontabilizar', component: ConsultaPendienteContabilizarComponent},
    { path: '', redirectTo: '/login', pathMatch: 'full' }, //route by default
];

@NgModule({
    imports: [RouterModule.forRoot(routes)],
    exports: [RouterModule],
})

export class AppRoutingModule {}