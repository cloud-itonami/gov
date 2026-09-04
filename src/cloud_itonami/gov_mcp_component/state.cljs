(ns cloud-itonami.gov-mcp-component.state
  "App state for the gov appview UI (gov-mcp-component). Single reagent atom
  mirroring the app descriptor that
  appview/gov-mcp-component/svelte/src/routes/+page.svelte rendered
  (project facts, routes, runtime bindings, source path)."
  (:require [reagent.core :as r]))

(defonce state
  (r/atom
   {:app {:title "Gov Mcp Component"
          :project "etzhayyim-project-gov"
          :name "gov-mcp-component"
          :kind "appview"
          :route-count 0
          :routes []
          :vars []
          :xrpc true
          :relative-path "60-apps/etzhayyim-project-gov/appview/gov-mcp-component/svelte/src/routes/+page.svelte"}}))
