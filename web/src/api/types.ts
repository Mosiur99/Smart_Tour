import type { components } from "./schema";

/** Short names for the generated API types. Never edit schema.d.ts by hand. */
type Schemas = components["schemas"];

export type Hub = Schemas["Hub"];
export type DestinationSummary = Schemas["DestinationSummary"];
export type DestinationPage = Schemas["DestinationPage"];
export type DestinationDetail = Schemas["DestinationDetail"];
export type Place = Schemas["Place"];
export type DestinationHotel = Schemas["DestinationHotel"];
export type TransportOption = Schemas["TransportOption"];
export type PlanRequestBody = Schemas["PlanRequestBody"];
export type PlanResponse = Schemas["PlanResponse"];
export type TripPlan = Schemas["TripPlan"];
export type DayPlan = Schemas["DayPlan"];
export type Journey = Schemas["Journey"];
export type Leg = Schemas["Leg"];
export type Stop = Schemas["Stop"];
export type MissingCost = Schemas["MissingCost"];
export type BudgetLevel = PlanRequestBody["budget"];
export type Interest = NonNullable<PlanRequestBody["interests"]>[number];
