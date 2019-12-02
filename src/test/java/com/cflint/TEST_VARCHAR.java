package com.cflint;

import com.cflint.api.CFLintAPI;
import com.cflint.api.CFLintResult;
import com.cflint.config.ConfigBuilder;
import com.cflint.exception.CFLintConfigurationException;
import com.cflint.exception.CFLintScanException;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class TEST_VARCHAR {

    private CFLintAPI cfBugs;

    @Before
    public void setUp() throws IOException, CFLintConfigurationException {
        final ConfigBuilder configBuilder = new ConfigBuilder().include("SQL_VARCHAR_DIMENSION");
        cfBugs = new CFLintAPI(configBuilder.build());
    }

    @Test
    public void goodCFML() throws CFLintScanException, IOException {

        final String cfcSrc = "<cffunction name=\"f_getPOSearch\" access=\"public\" returntype=\"query\">\n" +
            "        <cfset var results = '' >\n" +
            "\n" +
            "        <cfquery name=\"results\" datasource=\"#APPLICATION.mssage_dsn#\">\n" +
            "            SELECT <cfif Val(ARGUMENTS.p_limit) GT 0>TOP #ARGUMENTS.p_limit#</cfif>\n" +
            "                SUM( quantity * item_price ) AS mb_value,\n" +
            "                SUM( quantity ) AS total_qty\n" +
            "\n" +
            "            FROM ( /* Duplicates suppress (con_contacts & statuses_change_log) */\n" +
            "\n" +
            "                SELECT\n" +
            "                    po.po_id,\n" +
            "                    SUBSTRING(lead_ids,1,LEN(lead_ids)- 1) AS lead_id,\n" +
            "                CROSS APPLY\n" +
            "                    (\n" +
            "                        SELECT\n" +
            "                        CAST(lead_id AS VARCHAR) + ','\n" +
            "                        FROM\n" +
            "                            lead_leads AS lead WITH(NOLOCK)\n" +
            "                        WHERE\n" +
            "                            lead.po_id = po.po_id FOR XML PATH('')\n" +
            "                    )\n" +
            "                    D (lead_ids)\n" +
            "\n" +
            "                LEFT JOIN\n" +
            "                    statuses s WITH(NOLOCK) ON s.status_id = po.status_id\n" +
            "\n" +
            "                LEFT JOIN\n" +
            "                    statuses_change_log log WITH(NOLOCK) ON log.order_id = po.po_id AND log.new_status = po.status_id AND log.order_type = 'PO'\n" +
            "                <cfif isDefined('ARGUMENTS.p_show_pos_without_invoices_only')>\n" +
            "                    LEFT JOIN [po_invoice] inv ON inv.po_id = po.po_id\n" +
            "                        AND po.po_id LIKE '#ARGUMENTS.p_po_number#'\n" +
            "                </cfif>\n" +
            "                <cfif isDefined('ARGUMENTS.have_shipping_invoice') AND NOT ARGUMENTS.have_shipping_invoice >\n" +
            "                    LEFT JOIN\n" +
            "                        flex_relationship_values\n" +
            "                    ON\n" +
            "                        flex_relationship_values.flex_relationship_id = 39 /*Shipping Invoice to PO ID*/\n" +
            "                    AND\n" +
            "                        po.po_id = flex_relationship_values.flex_child_value\n" +
            "                    LEFT JOIN\n" +
            "                        shipping_invoice\n" +
            "                    ON\n" +
            "                        flex_relationship_values.flex_parent_value = shipping_invoice.id\n" +
            "                </cfif>\n" +
            "                <cfif ARGUMENTS.shipping_invoice EQ TRUE >\n" +
            "                    OUTER APPLY (\n" +
            "                        SELECT TOP 1\n" +
            "                            *\n" +
            "                        FROM\n" +
            "                            flex_relationship_values AS shipping_inv\n" +
            "                        WHERE\n" +
            "                            shipping_inv.flex_relationship_id = 39 /*Shipping Invoice to PO ID*/\n" +
            "                            AND\n" +
            "                            po.po_id = shipping_inv.flex_child_value\n" +
            "                    ) AS shipping_inv\n" +
            "                </cfif>\n" +
            "\n" +
            "                WHERE\n" +
            "                    po.po_id LIKE '#ARGUMENTS.p_po_number#%'\n" +
            "                AND\n" +
            "                    po.site_id IN (1,4,68)<!--- 1-SovaMax USA,4-SovaMax EU,68-SovaMax EU Non-Printing --->\n" +
            "                <cfif isDefined('ARGUMENTS.related_company_id') AND VAL(ARGUMENTS.related_company_id) GT 0>\n" +
            "                    AND\n" +
            "                        po.company_id = <cfqueryparam value = \"#ARGUMENTS.related_company_id#\" cfsqltype = \"cf_sql_integer\">\n" +
            "                </cfif>\n" +
            "                <cfif isDefined('ARGUMENTS.p_show_pos_without_invoices_only') AND ARGUMENTS.p_show_pos_without_invoices_only>\n" +
            "                    AND\n" +
            "                        inv.po_id is null\n" +
            "                </cfif>\n" +
            "                <cfif isDefined('ARGUMENTS.have_shipping_invoice') AND NOT ARGUMENTS.have_shipping_invoice >\n" +
            "                    AND\n" +
            "                        (flex_relationship_values.flex_parent_value IS NULL OR flex_relationship_values.flex_parent_value IS NOT NULL AND shipping_invoice.status_id = 263 /*Canceled*/)\n" +
            "                </cfif>\n" +
            "\n" +
            "                GROUP BY\n" +
            "                    po.po_id,\n" +
            "                    SUBSTRING(lead_ids,1,LEN(lead_ids)- 1),\n" +
            "                    po.site_id,\n" +
            "                    ss.site_name,\n" +
            "                    po.created,\n" +
            "                    poi.item_id,\n" +
            "                    poi.quantity,\n" +
            "                    poi.cost,\n" +
            "                    ip.item_price,\n" +
            "                    po.company_id,\n" +
            "                    sre.sage_roles_employee_type_id,\n" +
            "                    com.company_name,\n" +
            "                    cc.country_name,\n" +
            "                    sre.employee_name,\n" +
            "                    s.status_name\n" +
            "                     <cfif isDefined('ARGUMENTS.p_show_pos_without_invoices_only')>\n" +
            "                    ,inv.id\n" +
            "                    </cfif>\n" +
            "                    <cfif ARGUMENTS.shipping_invoice EQ TRUE >\n" +
            "                        ,shipping_inv.flex_parent_value\n" +
            "                    </cfif>\n" +
            "            ) q\n" +
            "\n" +
            "            GROUP BY\n" +
            "                po_id,\n" +
            "                lead_id,\n" +
            "                site_id,\n" +
            "                site_name,\n" +
            "                po_created,\n" +
            "                po_number,\n" +
            "                company_id,\n" +
            "                company_name,\n" +
            "                country_name,\n" +
            "                employee_name,\n" +
            "                status_name,\n" +
            "                log_created\n" +
            "                 <cfif isDefined('ARGUMENTS.p_show_pos_without_invoices_only')>\n" +
            "                    ,inv_id\n" +
            "                </cfif>\n" +
            "                <cfif ARGUMENTS.shipping_invoice EQ TRUE >\n" +
            "                    ,shipping_inv\n" +
            "                </cfif>\n" +
            "\n" +
            "            ORDER BY\n" +
            "                po_id DESC,\n" +
            "                lead_id\n" +
            "        </cfquery>\n" +
            "\n" +
            "        <cfreturn results>\n" +
            "    </cffunction>";
        CFLintResult lintresult = cfBugs.scan(cfcSrc, "test");
        Collection<List<BugInfo>> result = lintresult.getIssues().values();
//        assertEquals(0, result.size());
        String a = lintresult.getJSON(false);
        System.out.println(a);
    }
}