# AE2: Gadgetry

An extension mod for [Applied Energistics 2](https://github.com/AppliedEnergistics/Applied-Energistics-2) on NeoForge 1.21.1.

## Features

### Limit ME Interface
The primary feature of this mod is the **Limit ME Interface**, a specialized block designed for advanced automation control:

*   **Grid Node:** Connects directly to your AE2 network and requires a channel.
*   **Item Marking:** Mark up to 9 different items in its interface to define what it should handle.
*   **Quantity Limiting:** Set a specific numerical limit for each marked item.
*   **Smart Insertion:** When items are inserted into this block (via pipes, hoppers, or other machines), it will only allow them into the AE2 network if the total quantity of that item in the network is currently below your set limit.
*   **External Access:** It acts as a standard inventory (`IItemHandler`), allowing external systems to "see" and "extract" the marked items from your AE2 network.

## Installation & Development

This project uses NeoForge. To set up the development environment:

1.  Clone this repository.
2.  Open the project in your IDE (IntelliJ IDEA is recommended).
3.  Run `gradlew --refresh-dependencies` if needed.

## License

This project is licensed under the [MIT License](LICENSE).

---

### Additional Resources
- **Applied Energistics 2 API:** Included for development.
- **NeoForged Documentation:** [https://docs.neoforged.net/](https://docs.neoforged.net/)

