package restendpoint.ApiAutomatizacionJE2

import groovy.transform.BaseScript
import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import util.LectorProperties
import util.Log
import util.HttpRequest
import org.apache.commons.lang3.StringUtils
import org.jsoup.nodes.Element
import org.jsoup.Jsoup
import groovy.transform.Field

import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response
import javax.ws.rs.core.MediaType

@BaseScript CustomEndpointDelegate delegate

final String URLEAZYBI = "http://localhost:8080/plugins/servlet/eazybi/"

@Field Log oLog = new Log("restEndPointEazyBIaccounts", "Clonar cubos JE2", "admin")
@Field LectorProperties oLectorProperties = new LectorProperties("../restendpoint/ApiAutomatizacionJE2/properties/restEndPointAutomatizacionJe2.properties", oLog)

cloneJE2Cube(
        httpMethod: "POST", groups: ["jira-administrators"]
) { MultivaluedMap queryParams, String body ->

    oLog.escribirInicio()

    Integer codigo
    String msn = ""

    try {
        String autentificacionEazyBI = oLectorProperties.getProperty("autentificacionEazyBI")

        ArrayList<String> allAccountNames = queryParams.get("accountNames").toString().replace("[","").replace("]","").split(",")
        ArrayList<String> allProjectKeys = queryParams.get("projectKeys").toString().replace("[","").replace("]","").split(",")
        ArrayList<String> allProjectLeads = queryParams.get("projectLeads").toString().replace("[","").replace("]","").split(",")
        ArrayList<String> allTimeZones = queryParams.get("timeZone").toString().replace("[","").replace("]","").split(",")
        ArrayList<String> allImportTimes = queryParams.get("importTime").toString().replace("[","").replace("]","").split(",")
        ArrayList<String> allModels = queryParams.get("model").toString().replace("[","").replace("]","").split(",")

        Integer accountsCount = 0

        if (allAccountNames.size() >= 1 && allAccountNames != null) {

            for (String accountName in allAccountNames) {
                String projectKey = allProjectKeys.get(accountsCount)
                String projectLead = allProjectLeads.get(accountsCount)

                accountsCount += 1
                oLog.escribir("INFO> Creando Cubo " + accountsCount + "/" + allAccountNames.size() + " ("+accountName+") para proyecto "+projectKey+" (líder " + projectLead + ") con modelo " + allModels.get(0) + "> ")

                String url = URLEAZYBI + "accounts/"

                HashMap responseCreateCube = this.createCube(accountName, autentificacionEazyBI, url, msn, codigo)

                codigo = (Integer) responseCreateCube.codigo
                msn = msn + (String) responseCreateCube.msn
                String accountId = (String) responseCreateCube.accountId

                if (codigo == 200) {
                    HashMap responseCreateDataSource = this.createDataSource(autentificacionEazyBI, URLEAZYBI, accountId, msn, codigo)

                    codigo = (Integer) responseCreateDataSource.codigo
                    msn = msn + (String) responseCreateDataSource.msn
                    String dataSourceId = (String) responseCreateDataSource.dataSourceId

                    if (codigo == 200) {
                        String model = allModels.get(0)
                        String timeZone = allTimeZones.get(0)
                        String importTime = allImportTimes.get(0)

                        HashMap responseSourceDefinition = this.defineSourceSelection(autentificacionEazyBI, URLEAZYBI, accountId, dataSourceId, msn, codigo, model, projectKey, timeZone, importTime)

                        codigo = (Integer) responseSourceDefinition.codigo
                        msn = msn + (String) responseSourceDefinition.msn + "\n\n"

                        if (codigo == 200) {
                            HashMap responseUserRoles = this.defineUserRoles(autentificacionEazyBI, URLEAZYBI, accountId, projectLead, msn, codigo)

                            codigo = (Integer) responseUserRoles.codigo
                            msn = msn + (String) responseUserRoles.msn + "\n\n"
                        }
                    }
                }
            }
        } else {
            codigo = 400
            oLog.escribir("Error> Se debe indicar un Account Name")
            msn = "Bad Request"
        }
    } catch (Exception e) {
        codigo = 500
        oLog.escribir("Error> Compilacion Mensaje> " + e.getMessage())
        oLog.escribir("Error> Compilacion> " + e.getStackTrace())
        msn = e.getMessage()

    } finally {
        return Response.ok(msn).status(codigo).build()
    }
}

showCloneEazyBIAccounts() { MultivaluedMap queryParams ->

    def dialog = """
        <section role="dialog" id="sr-dialog" class="aui-layer aui-dialog2 aui-dialog2-medium" aria-hidden="true" data-aui-remove-on-hide="true">
            <header class="aui-dialog2-header">
                <h2 class="aui-dialog2-header-main">Clone EazyBI Accounts by Model</h2>
                <a class="aui-dialog2-header-close">
                    <span class="aui-icon aui-icon-small aui-iconfont-close-dialog">Close</span>
                </a>
            </header>
            <div class="aui-dialog2-content">
                <form id="cloneCubesForm" class="aui">
                    <div class="field-group">
                        <aui-label for="accountNames">Account Names <span class="aui-icon icon-required"></span></aui-label>
                        <input class="text medium-field" type="text"id="accountNames" name="accountNames" placeholder="Cubo 1, Cubo 2, ...">
                    </div>
                    <div class="field-group">
                        <aui-label for="projectKeys">Project Keys <span class="aui-icon icon-required"></span></aui-label>
                        <input class="text medium-field" type="text"id="projectKeys" name="projectKeys" placeholder="JESC, JEBACK, ...">
                    </div>
                    <div class="field-group">
                        <aui-label for="projectLeads">Project Leaders <span class="aui-icon icon-required"></span></aui-label>
                        <input class="text medium-field" type="text"id="projectLeads" name="projectLeads" placeholder="usernam1, username2, ...">
                    </div>
                    <div class="field-group">
                        <aui-label for="timeZone">Time Zone <span class="aui-icon icon-required"></span></aui-label>
                        <aui-select id="timeZone" name="timeZone">
                            <aui-option>Madrid</aui-option>
                            <aui-option>Rome</aui-option>
                            <aui-option>Mexico City</aui-option>
                            <aui-option>Lima</aui-option>
                            <aui-option>Santiago</aui-option>
                            <aui-option>Buenos Aires</aui-option>
                        </aui-select>
                    </div>
                    <div class="field-group">
                        <aui-label for="importTime">Regular import at <span class="aui-icon icon-required"></span></aui-label>
                        <aui-select id="importTime" name="importTime">
                            <aui-option>00:00</aui-option>
                            <aui-option>00:30</aui-option>
                            <aui-option>01:00</aui-option>
                            <aui-option>01:30</aui-option>
                            <aui-option>02:00</aui-option>
                            <aui-option>02:30</aui-option>
                            <aui-option>03:00</aui-option>
                            <aui-option>03:30</aui-option>
                        </aui-select>
                    </div>
                    <div class="field-group">
                        <aui-label for="model">Model <span class="aui-icon icon-required"></span></aui-label>
                        <aui-select id="model" name="model">
                            <aui-option>JEGenerica</aui-option>
                            <aui-option>JEOutsourcing</aui-option>
                            <aui-option>COM</aui-option>
                        </aui-select>
                    </div>
             	</form>
            </div>
            <footer class="aui-dialog2-footer">
                <div class="aui-dialog2-footer-actions">
                    <button class="aui-button" id="submit">Clone</button>
                </div>
            </footer>
        </section>
        
        <script>
            function submit(e) {
				e.preventDefault();
                \$.ajax({
                    url:'https://stepspre.everis.com/jiraito/rest/scriptrunner/latest/custom/cloneJE2Cube?'.concat(\$('#cloneCubesForm').serialize()),
                    headers: { 'X-Atlassian-Token': 'nocheck' },
                    type:'POST',
                    dataType: 'html',
    				contentType: 'application/x-www-form-urlencoded; charset=UTF-8',
                    success:function(data, textStatus, jqXHR ){
                        var myFlag = AJS.flag({
                            type: 'success',
                            body: data,
                        });
                    },
                    error: function(xhr, status, error) {
                        var myFlag = AJS.flag({
                            type: 'error',
                            body: xhr.responseText,
                        });
                    }
                });
            }
            var el = document.getElementById("submit");
            if (el.addEventListener)
                el.addEventListener("click", submit, false);
            else if (el.attachEvent)
                el.attachEvent('onclick', submit);
        </script>
        
    """

    Response.ok().type(MediaType.TEXT_HTML).entity(dialog.toString()).build()
}

/**
 *
 * @param accountName
 * @param autentificacionEazyBI
 * @param URLEAZYBI
 * @param msn
 * @param codigo
 * @return
 */
private HashMap createCube(String accountName, String autentificacionEazyBI, String URLEAZYBI, String msn, Integer codigo){

    String accountId = null

    try {
        accountName = accountName.trim()

        String requestBody = "account[name]="+accountName

        HashMap conPostCreateCube = HttpRequest.post(URLEAZYBI, requestBody, autentificacionEazyBI, HttpRequest.FORM_URL_ENCODED, oLog)

        if (conPostCreateCube.statusCode == 200) {
            String htmlResponsePost = conPostCreateCube.content

            if (htmlResponsePost.indexOf("must be unique") == -1){

                HashMap conGetCreatedCube = HttpRequest.get(URLEAZYBI, autentificacionEazyBI, oLog)
                String htmlResponseGet = conGetCreatedCube.content

                Element elementAccount = Jsoup.parse(htmlResponseGet).getElementsContainingOwnText(accountName).first()

                String accountUrl = elementAccount.attr("href")
                accountId = StringUtils.substringBetween(accountUrl, "accounts/", "/select")

                oLog.escribir("INFO> Cubo creado> " + accountName + ", con ID: " + accountId)

                codigo = 200
                msn = "Cubo creado> " + accountName + ", con ID: " + accountId

            } else {
                codigo = 500
                oLog.escribir("INFO> Cubo NO creado, ya existe> " + accountName)
                msn = "Cubo NO creado, Ya existe> " + accountName + "\n\n"
            }
        } else if (conPostCreateCube.statusCode == 404) {
            codigo = 404
            oLog.escribir("INFO> Error en la URL> " + URLEAZYBI)
            msn = "Error en la URL> " + URLEAZYBI + "\n\n"
        } else {
            oLog.escribir("ERROR> Cubo no creado> " + conPostCreateCube.statusCode + " - " + conPostCreateCube.message)
            msn = "Cubo NO creado> " + conPostCreateCube.message + "\n\n"
            codigo = conPostCreateCube.statusCode
        }

    } catch (Exception e) {
        codigo = 500
        oLog.escribir("Error> Compilacion en createCube> " + e.getMessage())
        msn = e.getMessage()
    } finally {
        HashMap response = new HashMap()
        response.put("codigo", codigo)
        response.put("msn", msn)
        response.put("accountId", accountId)
        return response
    }
}

/**
 *
 * @param autentificacionEazyBI
 * @param URLEAZYBI
 * @param accountId
 * @param msn
 * @param codigo
 * @return
 */
private HashMap createDataSource(String autentificacionEazyBI, String URLEAZYBI, String accountId, String msn, Integer codigo){

    String dataSourceId = null
    String requestBody = "source_application[application_type]=jira_local"
    String url = URLEAZYBI+"accounts/"+accountId+"/source_applications"

    try {
        HashMap conPostCreateDataSource = HttpRequest.post(url, requestBody, autentificacionEazyBI, HttpRequest.FORM_DATA, oLog)

        if (conPostCreateDataSource.statusCode == 200) {
            String htmlResponse = conPostCreateDataSource.content

            if (htmlResponse.indexOf("cannot create duplicate") == -1){

                HashMap conGetCreatedDataSource = HttpRequest.get(url, autentificacionEazyBI, oLog)

                dataSourceId = StringUtils.substringBetween(conGetCreatedDataSource.content, "SourceApplicationsList([{\"id\":", ",\"account_id\"")

                codigo = 200
                oLog.escribir("INFO> Data Source creado con ID> " + dataSourceId)
                msn = " > Data Source creado"

            } else {
                codigo = 500
                oLog.escribir("INFO> Data Source NO creado, ya existe uno> ")
                msn = " > Data Source NO creado, Ya existe uno" + "\n\n"
            }
        } else if (conPostCreateDataSource.statusCode == 404) {
            codigo = 404
            oLog.escribir("INFO> Error en la URL> " + url)
            msn = " > Error en la URL> " + url + "\n\n"
        } else {
            oLog.escribir("ERROR> Data Source no creado> " + conPostCreateDataSource.statusCode + " - " + conPostCreateDataSource.message)
            msn = " > Data Source NO creado> " + conPostCreateDataSource.message + "\n\n"
            codigo = conPostCreateDataSource.statusCode
        }

    } catch (Exception e) {
        codigo = 500
        oLog.escribir("Error> Compilacion en createDataSource> " + e.getMessage())
        msn = e.getMessage()
    } finally {
        HashMap response = new HashMap()
        response.put("codigo", codigo)
        response.put("msn", msn)
        response.put("dataSourceId", dataSourceId)
        return response
    }
}

/**
 *
 * @param autentificacionEazyBI
 * @param URLEAZYBI
 * @param accountId
 * @param dataSourceId
 * @param msn
 * @param codigo
 * @param modelo
 * @param projectKey
 * @return
 */
private HashMap defineSourceSelection(String autentificacionEazyBI, String URLEAZYBI, String accountId, String dataSourceId, String msn, Integer codigo, String modelo, String projectKey, String timeZone, String importTime){

    projectKey = projectKey.trim()

    String url = URLEAZYBI+"accounts/"+accountId+"/source_applications/"+dataSourceId+"/source_selection"

    String requestBody = "source_application[source_selection_ids][]="+ projectKey +
            "&source_application[extra_options][regular_import_frequency]=86400" + //el ID de este campo es el mismo en todas las instancias (at specific time)
            "&source_application[extra_options][time_zone]="+ timeZone +
            "&source_application[extra_options][regular_import_at]=" + importTime

    String cadenaPropsDimension = oLectorProperties.getProperty("camposDef"+modelo+"Dimensions")
    String cadenaPropsMeasure = oLectorProperties.getProperty("camposDef"+modelo+"Measures")
    String cadenaPropsProperty = oLectorProperties.getProperty("camposDef"+modelo+"Properties")
    String cadenaPropsValueCh = oLectorProperties.getProperty("camposDef"+modelo+"ValuesCh")

    if (cadenaPropsDimension != "" && cadenaPropsDimension.size() >= 1) {
        String[] dimensionsFieldsIDs = cadenaPropsDimension.split(",")

        for (String cfDimension : dimensionsFieldsIDs) {
            requestBody = requestBody + "&source_application[custom_fields][" + cfDimension + "][dimension]=1"
        }
    }

    if (cadenaPropsMeasure != "" && cadenaPropsMeasure.size() >= 1) {
        String[] measuresFieldsIDs = cadenaPropsMeasure.split(",")

        for (String cfMeasure : measuresFieldsIDs) {
            requestBody = requestBody + "&source_application[custom_fields][" + cfMeasure + "][measure]=1"
        }
    }

    if (cadenaPropsProperty != "" && cadenaPropsProperty.size() >= 1) {
        String[] propertiesFieldsIDs = cadenaPropsProperty.split(",")

        for (String cfProperty : propertiesFieldsIDs) {
            requestBody = requestBody + "&source_application[custom_fields][" + cfProperty + "][property]=1"
        }
    }

    if (cadenaPropsValueCh != "" && cadenaPropsValueCh.size() >= 1) {
        String[] valueChFieldsIDs = cadenaPropsValueCh.split(",")

        for (String cfValueCh : valueChFieldsIDs) {
            requestBody = requestBody + "&source_application[custom_fields][" + cfValueCh + "][changes]=1"
        }
    }

    requestBody = requestBody + oLectorProperties.getProperty("camposDef"+modelo+"ExtraOptions")

    requestBody = requestBody + "&commit=Import"

    try {
        oLog.escribir("REQUEST BODY: "+requestBody)

        HashMap conPutDefineSourceSelection = HttpRequest.put(url, requestBody, autentificacionEazyBI, HttpRequest.FORM_DATA, oLog)

        if (conPutDefineSourceSelection.statusCode == 404) {
            codigo = 200
            oLog.escribir("INFO> Source Selection definida> ")
            msn = " > Source Selection definida"
        } else {
            oLog.escribir("ERROR> Source Selection NO definida> " + conPutDefineSourceSelection.statusCode + " - " + conPutDefineSourceSelection.message)
            msn = " > Source Selection NO definida> " + conPutDefineSourceSelection.message + "\n\n"
            codigo = conPutDefineSourceSelection.statusCode
        }

    } catch (Exception e) {
        codigo = 500
        oLog.escribir("Error> Compilacion en defineSourceSelection> " + e.getMessage())
        msn = e.getMessage()
    } finally {
        HashMap response = new HashMap()
        response.put("codigo", codigo)
        response.put("msn", msn)
        return response
    }
}

/**
 *
 * @param autentificacionEazyBI
 * @param URLEAZYBI
 * @param accoundId
 * @param userName
 * @param msn
 * @param codigo
 * @return
 */
private HashMap defineUserRoles(String autentificacionEazyBI, String URLEAZYBI, String accoundId, String userName, String msn, Integer codigo){

    userName = userName.trim()

    String url = URLEAZYBI + "eazy/accounts/" + accoundId + "/account_users"

    String userRolesJiraAdminJSON = '{"role":"user_admin","group_name":"jira-administrators"}'
    String userRolesProjLeadJSON = '{"role":"reports_admin","external_id":"' + userName + '"}'

    try {

        HashMap conDefUserRolesPL = HttpRequest.post(url,userRolesProjLeadJSON,autentificacionEazyBI,HttpRequest.APP_JSON,oLog)

        if (conDefUserRolesPL.statusCode == 200) {

            HttpRequest.post(url,userRolesJiraAdminJSON,autentificacionEazyBI,HttpRequest.APP_JSON,oLog)

            codigo = 200
            oLog.escribir("INFO> Usuarios definidos> ")
            msn = " > Usuarios definidos"

        } else {
            oLog.escribir("ERROR> usuarios NO definidos> " + conDefUserRolesPL.statusCode + " - " + conDefUserRolesPL.message)
            msn = " > Usuarios NO definidos.> " + conDefUserRolesPL.message + "\n\n"
            codigo = conDefUserRolesPL.statusCode
        }

    } catch (Exception e) {
        codigo = 500
        oLog.escribir("Error> Compilacion en defineUserRoles> " + e.getMessage())
        msn = e.getMessage()
    } finally {
        HashMap response = new HashMap()
        response.put("codigo", codigo)
        response.put("msn", msn)
        return response
    }
}
