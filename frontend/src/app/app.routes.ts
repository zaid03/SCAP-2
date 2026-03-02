import { RouterModule, Routes } from '@angular/router';
import { NgModule } from '@angular/core';
import { LoginComponent } from './login/login.component';
import { EntComponent } from './ent/ent.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { ProveedoreesComponent } from './proveedorees/proveedorees.component';
import { CentrogestorComponent } from './centrogestor/centrogestor.component';
import { EjeComponent } from './eje/eje.component';
import { FacturasComponent } from './facturas/facturas.component';
import { CreditoComponent } from './credito/credito.component';
import { FamiliaComponent } from './familia/familia.component';
import { CgeComponent } from './cge/cge.component';
import { ServiciosComponent } from './servicios/servicios.component';
import { EntregaComponent } from './entrega/entrega.component';
import { CosteComponent } from './coste/coste.component';
import { PersonaComponent } from './persona/persona.component';
import { EjercicioComponent } from './ejercicio/ejercicio.component';
import { PersonasPorServiciosComponent } from './personas-por-servicios/personas-por-servicios.component';
import { ConsultaProveedoresComponent } from './consulta-proveedores/consulta-proveedores.component';
import { ConsultaFacturaComponent } from './consulta-factura/consulta-factura.component';
import { BolsaCreditoComponent } from './bolsa-credito/bolsa-credito.component';
import { ConsultaBolsasComponent } from './consulta-bolsas/consulta-bolsas.component';
import { ContratosComponent } from './contratos/contratos.component';
import { MonitorContabilizacionComponent } from './monitor-contabilizacion/monitor-contabilizacion.component';

export const routes: Routes = [
    { path: 'login', component: LoginComponent },
    { path: 'ent', component: EntComponent },
    { path: 'dashboard', component: DashboardComponent },
    { path: 'proveedorees', component: ProveedoreesComponent },
    { path: 'eje', component: EjeComponent},
    { path: 'centro-gestor', component: CentrogestorComponent},
    { path: 'facturas', component: FacturasComponent},
    { path: 'credito-Cge', component: CreditoComponent},
    { path: 'familia', component: FamiliaComponent},
    { path: 'centroGestor', component: CgeComponent},
    { path: 'servicios', component: ServiciosComponent},
    { path: 'entrega', component: EntregaComponent},
    { path: 'coste', component: CosteComponent},
    { path: 'persona', component: PersonaComponent},
    { path: 'ejercicios', component: EjercicioComponent},
    { path: 'personas-por-servicios', component: PersonasPorServiciosComponent },
    { path: 'Cproveedores', component: ConsultaProveedoresComponent},
    { path: 'Cfactura', component: ConsultaFacturaComponent},
    { path: 'credito', component: BolsaCreditoComponent},
    { path: 'Ccredito', component: ConsultaBolsasComponent},
    { path: 'contratos', component: ContratosComponent},
    { path: 'contabilizacion', component: MonitorContabilizacionComponent},
    { path: '', redirectTo: '/login', pathMatch: 'full' }, //route by default
];

@NgModule({
    imports: [RouterModule.forRoot(routes)],
    exports: [RouterModule],
})

export class AppRoutingModule {}